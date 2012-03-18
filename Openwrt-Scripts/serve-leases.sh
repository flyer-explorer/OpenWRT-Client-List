#!/bin/sh

if [ -e /etc/dhcp-clients.conf ]; then
    source /etc/dhcp-clients.conf
fi

if [ ! -e /etc/dhcp-clients.conf ] || [ -z "$PORT" ] || [ -z "$TRANSPORT" ] || [ -z "$UDP_RATE" ] || [ -z "$LEASE_FILE" ]; then
    echo "Missing configuration.  I will not run until you create it."
    echo " "
    echo "DO NOT EDIT $0, rather, create the file"
    echo "/etc/dhcp-clients.conf with the following sh paramters set:"
    echo 
    echo "PORT=<int>            TCP/UDP Port to send data over"
    echo "LEASE_FILE=<path>     Path to where the DNSMASQ lease file is located"
    echo "                      This is almost always /tmp/dhcp.leases"
    echo "TRANSPORT=<tcp|udp>   Defines how to transport the lease information."
    echo "                      \"tcp\" will use a standard socket on the specified port"
    echo "                      \"udp\" will echo the contents of \$LEASE_FILE as a" 
    echo "                      broadcast every \$UDP_RATE"
    echo "UDP_RATE=<int, secs>  How often (in seconds) should I broadcast the "
    echo "                      leases over the UDP Broadcast"
    echo " "
    echo "Example /etc/dhcp-clients.conf"
    echo " "
    echo "  PORT=2000"
    echo "  TRANSPORT=udp"
    echo "  UDP_RATE=1"
    echo "  LEASE_FILE=/tmp/dhcp.leases"
    echo " "
    echo " "
    exit 1
fi

if [ 1 -eq $# ] && [ $1 == "udp" ] && [ $TRANSPORT == "udp" ]; then
    echo "Broadcast Loop Starting"
    while [ 1 ]; do
        ncat -p $PORT -c "cat $LEASE_FILE" -u 255.255.255.255 $PORT 2> /dev/null &
        sleep $UDP_RATE
    done
    exit 1
else
    if    [ $TRANSPORT == "tcp" ]; then
        echo "Starting TCP Server"
        ncat -v -k -l -p $PORT -c "cat $LEASE_FILE" 2> /dev/null & 
    elif  [ $TRANSPORT == "udp" ]; then
        echo "Restarting Script in Broadcast loop"
        ( $0 udp & )
    fi
fi

