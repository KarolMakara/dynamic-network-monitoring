from mininet.topo import Topo
from mininet.link import TCLink


class MonitoringTopo(Topo):
    def __init__(self):

        Topo.__init__(self)

        h1 = self.addHost('h1', ip='10.0.0.1/24')
        h2 = self.addHost('h2', ip='10.0.0.2/24')
        h3 = self.addHost('h3', ip='10.0.0.3/24')
        h4 = self.addHost('h4', ip='10.0.0.4/24')
        s1 = self.addSwitch('s1')
        s2 = self.addSwitch('s2')
        s3 = self.addSwitch('s3')
        s4 = self.addSwitch('s4')


        a1 = self.addSwitch('a1')
        a2 = self.addSwitch('a2')


        self.addLink(h1, s1, cls=TCLink, bw=100)
        self.addLink(h2, s2, cls=TCLink, bw=100)
        self.addLink(h3, s3, cls=TCLink, bw=100)
        self.addLink(h4, s4, cls=TCLink, bw=100)


        self.addLink(s1, a1, cls=TCLink, bw=1000)
        self.addLink(s1, a2, cls=TCLink, bw=1000)

        self.addLink(s2, a1, cls=TCLink, bw=1000)
        self.addLink(s2, a2, cls=TCLink, bw=1000)


        self.addLink(a1, s3, cls=TCLink, bw=1000)
        self.addLink(a2, s4, cls=TCLink, bw=1000)


        self.addLink(a1, a2, cls=TCLink, bw=1000)


topos = { 'monitoringtopo': ( lambda: MonitoringTopo() ) }
