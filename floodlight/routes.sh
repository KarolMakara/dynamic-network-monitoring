#!/bin/bash
# routes.sh - Static flow rules for monitoring topology
# Implements manual Spanning Tree to avoid loops.

echo "Installing static flow rules for monitoring topology..."

# Base URL for Floodlight REST API
FL_URL="http://localhost:8080/wm/staticflowpusher/json"

# --- Switch S1 (01) ---
# Connects h1 (P1) <-> a1 (P2). P3 (to a2) is BLOCKED.
curl -s -X POST -d '{
  "switch":"00:00:00:00:00:00:00:01",
  "name":"s1-h1-uplink",
  "priority":"32768",
  "active":"true",
  "in_port":"1", 
  "actions":"output=2"
}' $FL_URL

curl -s -X POST -d '{
  "switch":"00:00:00:00:00:00:00:01",
  "name":"s1-uplink-h1",
  "priority":"32768",
  "active":"true",
  "in_port":"2", 
  "actions":"output=1"
}' $FL_URL

# --- Switch S2 (02) ---
# Connects h2 (P1) <-> a1 (P2). P3 (to a2) is BLOCKED.
curl -s -X POST -d '{
  "switch":"00:00:00:00:00:00:00:02",
  "name":"s2-h2-uplink",
  "priority":"32768",
  "active":"true",
  "in_port":"1", 
  "actions":"output=2"
}' $FL_URL

curl -s -X POST -d '{
  "switch":"00:00:00:00:00:00:00:02",
  "name":"s2-uplink-h2",
  "priority":"32768",
  "active":"true",
  "in_port":"2", 
  "actions":"output=1"
}' $FL_URL

# --- Switch S3 (03) ---
# Connects h3 (P1) <-> a1 (P2).
curl -s -X POST -d '{
  "switch":"00:00:00:00:00:00:00:03",
  "name":"s3-h3-uplink",
  "priority":"32768",
  "active":"true",
  "in_port":"1", 
  "actions":"output=2"
}' $FL_URL

curl -s -X POST -d '{
  "switch":"00:00:00:00:00:00:00:03",
  "name":"s3-uplink-h3",
  "priority":"32768",
  "active":"true",
  "in_port":"2", 
  "actions":"output=1"
}' $FL_URL

# --- Switch S4 (04) ---
# Connects h4 (P1) <-> a2 (P2).
curl -s -X POST -d '{
  "switch":"00:00:00:00:00:00:00:04",
  "name":"s4-h4-uplink",
  "priority":"32768",
  "active":"true",
  "in_port":"1", 
  "actions":"output=2"
}' $FL_URL

curl -s -X POST -d '{
  "switch":"00:00:00:00:00:00:00:04",
  "name":"s4-uplink-h4",
  "priority":"32768",
  "active":"true",
  "in_port":"2", 
  "actions":"output=1"
}' $FL_URL

# --- Switch A1 (05) ---
# Core switch. Connects s1(P1), s2(P2), s3(P3), a2(P4).
# Simple flooding between these active ports allows full connectivity.
curl -s -X POST -d '{
  "switch":"00:00:00:00:00:00:00:05",
  "name":"a1-1-to-others",
  "priority":"32768",
  "active":"true",
  "in_port":"1",
  "actions":"output=2,output=3,output=4"
}' $FL_URL

curl -s -X POST -d '{
  "switch":"00:00:00:00:00:00:00:05",
  "name":"a1-2-to-others",
  "priority":"32768",
  "active":"true",
  "in_port":"2",
  "actions":"output=1,output=3,output=4"
}' $FL_URL

curl -s -X POST -d '{
  "switch":"00:00:00:00:00:00:00:05",
  "name":"a1-3-to-others",
  "priority":"32768",
  "active":"true",
  "in_port":"3",
  "actions":"output=1,output=2,output=4"
}' $FL_URL

curl -s -X POST -d '{
  "switch":"00:00:00:00:00:00:00:05",
  "name":"a1-4-to-others",
  "priority":"32768",
  "active":"true",
  "in_port":"4",
  "actions":"output=1,output=2,output=3"
}' $FL_URL

# Allow return traffic from a2 on s1
curl -s -X POST -d '{
  "switch":"00:00:00:00:00:00:00:01",
  "name":"s1-uplink-backup-h1",
  "priority":"32768",
  "active":"true",
  "in_port":"3", 
  "actions":"output=1"
}' $FL_URL

# Allow return traffic from a2 on s2
curl -s -X POST -d '{
  "switch":"00:00:00:00:00:00:00:02",
  "name":"s2-uplink-backup-h2",
  "priority":"32768",
  "active":"true",
  "in_port":"3", 
  "actions":"output=1"
}' $FL_URL


# --- Switch A2 (06) ---
# Fully enabled bridge. Connects s1(P1), s2(P2), s4(P3), a1(P4).
# Explicit forwarding to all other ports.

curl -s -X POST -d '{
  "switch":"00:00:00:00:00:00:00:06",
  "name":"a2-1-to-others",
  "priority":"32768",
  "active":"true",
  "in_port":"1", 
  "actions":"output=2,output=3,output=4"
}' $FL_URL

curl -s -X POST -d '{
  "switch":"00:00:00:00:00:00:00:06",
  "name":"a2-2-to-others",
  "priority":"32768",
  "active":"true",
  "in_port":"2", 
  "actions":"output=1,output=3,output=4"
}' $FL_URL

curl -s -X POST -d '{
  "switch":"00:00:00:00:00:00:00:06",
  "name":"a2-3-to-others",
  "priority":"32768",
  "active":"true",
  "in_port":"3", 
  "actions":"output=1,output=2,output=4"
}' $FL_URL

curl -s -X POST -d '{
  "switch":"00:00:00:00:00:00:00:06",
  "name":"a2-4-to-others",
  "priority":"32768",
  "active":"true",
  "in_port":"4", 
  "actions":"output=1,output=2,output=3"
}' $FL_URL


echo ""
echo "✓ Loop-free static routes installed."
echo ""
