#!/usr/bin/env python3

import os
import subprocess
import time
import sys

HOSTS = ["h1", "h2", "h3", "h4"]
PORT = 5001

SCAPY_SCRIPT_PATH = "/tmp/mn_traffic_gen.py"

SCAPY_CODE = r"""
import sys, random
from scapy.all import *

targets = sys.argv[1].split(',')
my_port = int(sys.argv[2])
payload = b"X" * 1400

my_iface = [i for i in get_if_list() if "-eth0" in i][0]

conf.verb = 0
sock = conf.L2socket(iface=my_iface)

pkts = [Ether(dst="ff:ff:ff:ff:ff:ff")/IP(dst=t)/UDP(dport=my_port)/Raw(load=payload) for t in targets]

print("Start high-speed traffic on %s" % my_iface)

while True:
    try:
        sock.send(random.choice(pkts))
        time.sleep(0.0001) # around 33 Mb/s
    except:
        pass
"""

def get_host_pid(host):
    try:
        cmd = "pgrep -f 'mininet:{}'".format(host)
        out = subprocess.check_output(cmd, shell=True)
        if hasattr(out, 'decode'):
            out = out.decode('utf-8')
        pid = out.strip().split('\n')[0]
        return pid
    except subprocess.CalledProcessError:
        print("Error: Host {} not found. Is Mininet running?".format(host))
        return None

def setup_script():
    with open(SCAPY_SCRIPT_PATH, "w") as f:
        f.write(SCAPY_CODE)

def start_generators():
    setup_script()
    print("=== STARTING GENERATORS ===")

    processes = []

    for i, h in enumerate(HOSTS):
        targets = ["10.0.0.{}".format(j+1) for j in range(len(HOSTS)) if HOSTS[j] != h]
        target_str = ",".join(targets)

        pid = get_host_pid(h)
        if not pid:
            continue

        cmd = ["mnexec", "-a", pid, "python3", SCAPY_SCRIPT_PATH, target_str, str(PORT)]

        print("[{} PID={}] -> Targets: {}".format(h, pid, targets))

        devnull = open(os.devnull, 'w')
        p = subprocess.Popen(cmd, stdout=devnull, stderr=devnull)
        processes.append(p)

    print("=== Started {} generators in background ===".format(len(processes)))
    print("Press Ctrl+C to stop the generators...")

    try:
        while True:
            time.sleep(1)
    except KeyboardInterrupt:
        print("\nShutting down generators...")
        for p in processes:
            p.terminate()

if __name__ == "__main__":
    if os.geteuid() != 0:
        print("This script must be run as ROOT (sudo).")
        exit(1)

    start_generators()
