##PERSONAL COMPUTER IP -> 74.83.199.35

#SSH on crawler
sudo ufw allow OpenSSH

#Allow HTTP on crawler
sudo ufw allow from 192.168.1.100 to any port 80 proto tcp
sudo ufw allow from 192.168.1.100 to any port 22+

ufw allow from IPADDRESS to any port 3306
ufw allow from IPADDRESS to any port 27017