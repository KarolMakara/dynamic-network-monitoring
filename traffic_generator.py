#!/usr/bin/env python

import os
import subprocess
import time
import sys

HOSTS = ["h1", "h2", "h3", "h4"]
PORT = 5001

SCAPY_SCRIPT_PATH = "/tmp/mn_traffic_gen.py"

SCAPY_CODE = r"""
import sys, time, random
from scapy.all import *

targets = sys.argv[1].split(',')
my_port = int(sys.argv[2])

print("Start traffic to %s" % targets)

while True:
    try:
        dst = random.choice(targets)
        payload = "X" * random.randint(50, 500)
        
        pkt = IP(dst=dst)/UDP(dport=my_port)/Raw(load=payload)
        send(pkt, verbose=False)
        
        time.sleep(random.uniform(0.05, 0.2))
    except Exception as e:
        print("Error sending: %s" % e)
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
        print("Blad: Nie znaleziono hosta {}. Czy Mininet dziala?".format(host))
        return None

def setup_script():
    with open(SCAPY_SCRIPT_PATH, "w") as f:
        f.write(SCAPY_CODE)

def start_generators():
    setup_script()
    print("=== STARTUJE GENERATORY (Skrypt: {}) ===".format(SCAPY_SCRIPT_PATH))

    processes = []

    for i, h in enumerate(HOSTS):
        targets = ["10.0.0.{}".format(j+1) for j in range(len(HOSTS)) if HOSTS[j] != h]
        target_str = ",".join(targets)

        pid = get_host_pid(h)
        if not pid:
            continue
            
        cmd = ["mnexec", "-a", pid, "python", SCAPY_SCRIPT_PATH, target_str, str(PORT)]
        
        print("[{} PID={}] -> Cele: {}".format(h, pid, targets))
        
        devnull = open(os.devnull, 'w')
        p = subprocess.Popen(cmd, stdout=devnull, stderr=devnull)
        processes.append(p)

    print("=== Uruchomiono {} generatorow w tle ===".format(len(processes)))
    print("Nacisnij Ctrl+C, aby zatrzymac generatory...")
    
    try:
        while True:
            time.sleep(1)
    except KeyboardInterrupt:
        print("\nZamykanie generatorow...")
        for p in processes:
            p.terminate()

if __name__ == "__main__":
    if os.geteuid() != 0:
        print("Ten skrypt musi byc uruchomiony jako ROOT (sudo).")
        exit(1)
        
    start_generators()
