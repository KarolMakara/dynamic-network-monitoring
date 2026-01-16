#!/bin/bash

GREEN='\033[0;32m'
BLUE='\033[0;34m'
NC='\033[0m'

echo -e "${BLUE}=== Rozpoczynam konfigurację środowiska Floodlight na Ubuntu 24.04 ===${NC}"

echo -e "${GREEN}[1/6] Dodawanie PPA dla OpenJDK 8...${NC}"
sudo apt update
sudo apt install -y software-properties-common
sudo add-apt-repository -y ppa:openjdk-r/ppa
sudo apt update

echo -e "${GREEN}[2/6] Instalacja OpenJDK 8 i 21...${NC}"
sudo apt install -y openjdk-8-jdk openjdk-21-jdk

echo -e "${GREEN}[3/6] Instalacja Ant, Maven i Git...${NC}"
sudo apt install -y ant maven git build-essential mininet curl

echo -e "${GREEN}[4/6] Konfiguracja update-alternatives dla java i javac...${NC}"
sudo update-alternatives --install /usr/bin/java java /usr/lib/jvm/java-8-openjdk-amd64/bin/java 1081
sudo update-alternatives --install /usr/bin/javac javac /usr/lib/jvm/java-8-openjdk-amd64/bin/javac 1081

sudo update-alternatives --install /usr/bin/java java /usr/lib/jvm/java-21-openjdk-amd64/bin/java 1101
sudo update-alternatives --install /usr/bin/javac javac /usr/lib/jvm/java-21-openjdk-amd64/bin/javac 1101

echo -e "${GREEN}[5/6] Ustawianie Java 8 jako domyślnej...${NC}"
sudo update-alternatives --set java /usr/lib/jvm/java-8-openjdk-amd64/bin/java
sudo update-alternatives --set javac /usr/lib/jvm/java-8-openjdk-amd64/bin/javac

echo -e "${GREEN}[6/6] Konfiguracja JAVA_HOME w ~/.bashrc...${NC}"
if ! grep -q "JAVA_HOME" ~/.bashrc; then
    echo 'export JAVA_HOME=/usr/lib/jvm/java-8-openjdk-amd64' >> ~/.bashrc
    echo 'export PATH=$JAVA_HOME/bin:$PATH' >> ~/.bashrc
fi

echo -e "${BLUE}=== Instalacja zakończona! ===${NC}"
echo -e "Zrestartuj terminal lub wpisz: ${GREEN}source ~/.bashrc${NC}"
echo -e "Sprawdź wersję: ${GREEN}java -version${NC}"
