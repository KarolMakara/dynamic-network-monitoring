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

Access network monitor dashboard with: http://<vm_ip>:8080/wm/networkmonitor/dashboard

#### Run traffic generator with:
```bash
$ sudo python3 traffic_generator.py
```
