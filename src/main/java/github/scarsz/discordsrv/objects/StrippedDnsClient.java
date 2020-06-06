/*
 * DiscordSRV - A Minecraft to Discord and back link plugin
 * Copyright (C) 2016-2020 Austin "Scarsz" Shapiro
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package github.scarsz.discordsrv.objects;

import org.minidns.AbstractDnsClient;
import org.minidns.MiniDnsFuture;
import org.minidns.dnsmessage.DnsMessage;
import org.minidns.dnsserverlookup.DnsServerLookupMechanism;
import org.minidns.util.InetAddressUtil;
import org.minidns.util.MultipleIoException;

import java.io.IOException;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Level;

/**
 * This is {@link org.minidns.DnsClient} without the extra garbage that causes major issues for some users
 */
public class StrippedDnsClient extends AbstractDnsClient {

    private static final List<DnsServerLookupMechanism> LOOKUP_MECHANISMS = new CopyOnWriteArrayList<>();

    private static final Set<String> blacklistedDnsServers = Collections.newSetFromMap(new ConcurrentHashMap<>(4));

    private final Set<InetAddress> nonRaServers = Collections.newSetFromMap(new ConcurrentHashMap<>(4));

    private boolean askForDnssec = false;
    private boolean disableResultFilter = false;

    public StrippedDnsClient() {
        super();
    }

    @Override
    protected DnsMessage.Builder newQuestion(DnsMessage.Builder message) {
        message.setRecursionDesired(true);
        message.getEdnsBuilder().setUdpPayloadSize(dataSource.getUdpPayloadSize()).setDnssecOk(askForDnssec);
        return message;
    }

    private List<InetAddress> getServerAddresses() {
        return findDnsAddresses();
    }

    @Override
    public DnsMessage query(DnsMessage.Builder queryBuilder) throws IOException {
        DnsMessage q = newQuestion(queryBuilder).build();
        // While this query method does in fact re-use query(Question, String)
        // we still do a cache lookup here in order to avoid unnecessary
        // findDNS()calls, which are expensive on Android. Note that we do not
        // put the results back into the Cache, as this is already done by
        // query(Question, String).
        DnsMessage responseMessage = (cache == null) ? null : cache.get(q);
        if (responseMessage != null) {
            return responseMessage;
        }

        List<InetAddress> dnsServerAddresses = getServerAddresses();

        List<IOException> ioExceptions = new ArrayList<>(dnsServerAddresses.size());
        for (InetAddress dns : dnsServerAddresses) {
            if (nonRaServers.contains(dns)) {
                LOGGER.finer("Skipping " + dns + " because it was marked as \"recursion not available\"");
                continue;
            }

            try {
                responseMessage = query(q, dns);
                if (responseMessage == null) {
                    continue;
                }

                if (!responseMessage.recursionAvailable) {
                    boolean newRaServer = nonRaServers.add(dns);
                    if (newRaServer) {
                        LOGGER.warning("The DNS server "
                                + dns
                                + " returned a response without the \"recursion available\" (RA) flag set. This likely indicates a misconfiguration because the server is not suitable for DNS resolution");
                    }
                    continue;
                }

                if (disableResultFilter) {
                    return responseMessage;
                }

                switch (responseMessage.responseCode) {
                    case NO_ERROR:
                    case NX_DOMAIN:
                        break;
                    default:
                        String warning = "Response from " + dns + " asked for " + q.getQuestion() + " with error code: "
                                + responseMessage.responseCode + '.';
                        if (!LOGGER.isLoggable(Level.FINE)) {
                            // Only append the responseMessage is log level is not fine. If it is fine or higher, the
                            // response has already been logged.
                            warning += "\n" + responseMessage;
                        }
                        LOGGER.warning(warning);
                        // TODO Create new IOException and add to ioExceptions.
                        continue;
                }

                return responseMessage;
            } catch (IOException ioe) {
                ioExceptions.add(ioe);
            }
        }
        MultipleIoException.throwIfRequired(ioExceptions);
        // TODO assert that we never return null here.
        return null;
    }

    @Override
    protected MiniDnsFuture<DnsMessage, IOException> queryAsync(DnsMessage.Builder queryBuilder) {
        DnsMessage q = newQuestion(queryBuilder).build();
        // While this query method does in fact re-use query(Question, String)
        // we still do a cache lookup here in order to avoid unnecessary
        // findDNS()calls, which are expensive on Android. Note that we do not
        // put the results back into the Cache, as this is already done by
        // query(Question, String).
        DnsMessage responseMessage = (cache == null) ? null : cache.get(q);
        if (responseMessage != null) {
            return MiniDnsFuture.from(responseMessage);
        }

        final List<InetAddress> dnsServerAddresses = getServerAddresses();

        final MiniDnsFuture.InternalMiniDnsFuture<DnsMessage, IOException> future = new MiniDnsFuture.InternalMiniDnsFuture<>();
        final List<IOException> exceptions = Collections.synchronizedList(new ArrayList<IOException>(dnsServerAddresses.size()));

        // Filter loop.
        Iterator<InetAddress> it = dnsServerAddresses.iterator();
        while (it.hasNext()) {
            InetAddress dns = it.next();
            if (nonRaServers.contains(dns)) {
                it.remove();
                LOGGER.finer("Skipping " + dns + " because it was marked as \"recursion not available\"");
                continue;
            }
        }

        List<MiniDnsFuture<DnsMessage, IOException>> futures = new ArrayList<>(dnsServerAddresses.size());
        // "Main" loop.
        for (InetAddress dns : dnsServerAddresses) {
            if (future.isDone()) {
                for (MiniDnsFuture<DnsMessage, IOException> futureToCancel : futures) {
                    futureToCancel.cancel(true);
                }
                break;
            }

            MiniDnsFuture<DnsMessage, IOException> f = queryAsync(q, dns);
            f.onSuccess(future::setResult);
            f.onError(exception -> {
                exceptions.add(exception);
                if (exceptions.size() == dnsServerAddresses.size()) {
                    future.setException(MultipleIoException.toIOException(exceptions));
                }
            });
            futures.add(f);
        }

        return future;
    }

    /**
     * Retrieve a list of currently configured DNS servers IP addresses. This method does verify that only IP addresses are returned and
     * nothing else (e.g. DNS names).
     * <p>
     * The addresses are discovered by using one (or more) of the configured {@link DnsServerLookupMechanism}s.
     * </p>
     *
     * @return A list of DNS server IP addresses configured for this system.
     */
    public static List<String> findDNS() {
        List<String> res = null;
        for (DnsServerLookupMechanism mechanism : LOOKUP_MECHANISMS) {
            res = mechanism.getDnsServerAddresses();
            if (res == null) {
                continue;
            }

            assert(!res.isEmpty());

            // We could cache if res only contains IP addresses and avoid the verification in case. Not sure if its really that beneficial
            // though, because the list returned by the server mechanism is rather short.

            // Verify the returned DNS servers: Ensure that only valid IP addresses are returned. We want to avoid that something else,
            // especially a valid DNS name is returned, as this would cause the following String to InetAddress conversation using
            // getByName(String) to cause a DNS lookup, which would be performed outside of the realm of MiniDNS and therefore also outside
            // of its DNSSEC guarantees.
            Iterator<String> it = res.iterator();
            while (it.hasNext()) {
                String potentialDnsServer = it.next();
                if (!InetAddressUtil.isIpAddress(potentialDnsServer)) {
                    LOGGER.warning("The DNS server lookup mechanism '" + mechanism.getName()
                            + "' returned an invalid non-IP address result: '" + potentialDnsServer + "'");
                    it.remove();
                } else if (blacklistedDnsServers.contains(potentialDnsServer)) {
                    LOGGER.fine("The DNS server lookup mechanism '" + mechanism.getName()
                            + "' returned a blacklisted result: '" + potentialDnsServer + "'");
                    it.remove();
                }
            }

            if (!res.isEmpty()) {
                break;
            } else {
                LOGGER.warning("The DNS server lookup mechanism '" + mechanism.getName()
                        + "' returned not a single valid IP address after sanitization");
            }
        }

        return res;
    }

    /**
     * Retrieve a list of currently configured DNS server addresses.
     * <p>
     * Note that unlike {@link #findDNS()}, the list returned by this method
     * will take the IP version setting into account, and order the list by the
     * preferred address types (IPv4/v6). The returned list is modifiable.
     * </p>
     *
     * @return A list of DNS server addresses.
     * @see #findDNS()
     */
    public static List<InetAddress> findDnsAddresses() {
        // The findDNS() method contract guarantees that only IP addresses will be returned.
        List<String> res = findDNS();

        if (res == null) {
            return new ArrayList<>();
        }

        final IpVersionSetting setting = DEFAULT_IP_VERSION_SETTING;

        List<Inet4Address> ipv4DnsServer = null;
        List<Inet6Address> ipv6DnsServer = null;
        if (setting.v4) {
            ipv4DnsServer = new ArrayList<>(res.size());
        }
        if (setting.v6) {
            ipv6DnsServer = new ArrayList<>(res.size());
        }

        for (String dnsServerString : res) {
            // The following invariant must hold: "dnsServerString is a IP address". Therefore findDNS() must only return a List of Strings
            // representing IP addresses. Otherwise the following call of getByName(String) may perform a DNS lookup without MiniDNS being
            // involved. Something we want to avoid.
            assert (InetAddressUtil.isIpAddress(dnsServerString));

            InetAddress dnsServerAddress;
            try {
                dnsServerAddress = InetAddress.getByName(dnsServerString);
            } catch (UnknownHostException e) {
                LOGGER.log(Level.SEVERE, "Could not transform '" + dnsServerString + "' to InetAddress", e);
                continue;
            }
            if (dnsServerAddress instanceof Inet4Address) {
                if (!setting.v4) {
                    continue;
                }
                Inet4Address ipv4DnsServerAddress = (Inet4Address) dnsServerAddress;
                ipv4DnsServer.add(ipv4DnsServerAddress);
            } else if (dnsServerAddress instanceof Inet6Address) {
                if (!setting.v6) {
                    continue;
                }
                Inet6Address ipv6DnsServerAddress = (Inet6Address) dnsServerAddress;
                ipv6DnsServer.add(ipv6DnsServerAddress);
            } else {
                throw new AssertionError("The address '" + dnsServerAddress + "' is neither of type Inet(4|6)Address");
            }
        }

        List<InetAddress> dnsServers = new LinkedList<>();

        switch (setting) {
            case v4v6:
                dnsServers.addAll(ipv4DnsServer);
                dnsServers.addAll(ipv6DnsServer);
                break;
            case v6v4:
                dnsServers.addAll(ipv6DnsServer);
                dnsServers.addAll(ipv4DnsServer);
                break;
            case v4only:
                dnsServers.addAll(ipv4DnsServer);
                break;
            case v6only:
                dnsServers.addAll(ipv6DnsServer);
                break;
        }
        return dnsServers;
    }

    public static void addDnsServerLookupMechanism(DnsServerLookupMechanism dnsServerLookup) {
        if (!dnsServerLookup.isAvailable()) {
            LOGGER.fine("Not adding " + dnsServerLookup.getName() + " as it is not available.");
            return;
        }
        synchronized (LOOKUP_MECHANISMS) {
            // We can't use Collections.sort(CopyOnWriteArrayList) with Java 7. So we first create a temp array, sort it, and replace
            // LOOKUP_MECHANISMS with the result. For more information about the Java 7 Collections.sort(CopyOnWriteArrayList) issue see
            // http://stackoverflow.com/a/34827492/194894
            // TODO: Remove that workaround once MiniDNS is Java 8 only.
            ArrayList<DnsServerLookupMechanism> tempList = new ArrayList<>(LOOKUP_MECHANISMS.size() + 1);
            tempList.addAll(LOOKUP_MECHANISMS);
            tempList.add(dnsServerLookup);

            // Sadly, this Collections.sort() does not with the CopyOnWriteArrayList on Java 7.
            Collections.sort(tempList);

            LOOKUP_MECHANISMS.clear();
            LOOKUP_MECHANISMS.addAll(tempList);
        }
    }

    public static boolean removeDNSServerLookupMechanism(DnsServerLookupMechanism dnsServerLookup) {
        synchronized (LOOKUP_MECHANISMS) {
            return LOOKUP_MECHANISMS.remove(dnsServerLookup);
        }
    }

    public static boolean addBlacklistedDnsServer(String dnsServer) {
        return blacklistedDnsServers.add(dnsServer);
    }

    public static boolean removeBlacklistedDnsServer(String dnsServer) {
        return blacklistedDnsServers.remove(dnsServer);
    }

    public boolean isAskForDnssec() {
        return askForDnssec;
    }

    public void setAskForDnssec(boolean askForDnssec) {
        this.askForDnssec = askForDnssec;
    }

    public boolean isDisableResultFilter() {
        return disableResultFilter;
    }

    public void setDisableResultFilter(boolean disableResultFilter) {
        this.disableResultFilter = disableResultFilter;
    }

}
