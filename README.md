# Dynamic network traffic measurements (Floodlight)

### --- Prerequisites ---

Virtualbox image: Ubuntu 24.04 \
Install all required software, and setup java 8 with bewlow script (only once in lifetime of virtual machine):
```bash
$ ./setup_vm.sh
```

### --- How to clone and build the project ---

```bash
$ git clone https://github.com/KarolMakara/dynamic-network-monitoring.git
$ cd dynamic-network-monitoring/floodlight
$ ant clean && ant
```

### --- How to run the project ---

#### Run floodlight controller with:
```bash
$ ./run.sh
```

#### Run mininet with:
```bash
$ sudo mn --custom topology.py --topo monitoringtopo --controller=remote,ip=127.0.0.1 --mac --arp --switch ovs,failmode=standalone,protocols=OpenFlow10
```

#### Install static routes with:
```bash
$ ./routes.sh
```

Access network monitor dashboard with: http://localhost:8080/wm/networkmonitor/dashboard

#### Run traffic generator with:
```bash
$ sudo python3 traffic_generator.py
```


### Monitoring parameters

Low traffic polling: 5000 ms - time interval throughput calculating when traffic is low
High traffic polling: 1000 ms - time interval throughput calculating when traffic is high
Low traffic threshold: 10.0 Mb/s - low traffic threshold, uses low traffic poling time interval
High traffic threshold: 20.0 Mb/s - high traffic threshold, uses low traffic poling time interval, everything below this threshold is considered LOW traffic, everything beyon is HIGH
Analysis window: 60000 ms - every this time alghoritm is checking if traffic level is LOW or HIGH and sets current traffic polling to low or high traffic polling time interval
Monitoring switches: a1 (00:00:00:00:00:00:00:05), a2 (00:00:00:00:00:00:00:06) - DPID of agreggations switches, where current throughput is calculated
