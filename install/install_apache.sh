###setup apache2
apt-get -y install apache2 libapache2-mod-jk libapache2-mod-fcgid
usermod --append --groups tomcat,ctsms www-data
cp /ctsms/install/00_ctsms_http.conf /etc/apache2/sites-available/00_ctsms_http.conf
cp /ctsms/install/00_ctsms_https.conf /etc/apache2/sites-available/00_ctsms_https.conf
cp /ctsms/install/ports.conf /etc/apache2/ports.conf
wget https://raw.githubusercontent.com/phoenixctms/install-debian/$TAG/apache/blocklist.conf -O /etc/apache2/blocklist.conf
wget https://raw.githubusercontent.com/phoenixctms/install-debian/$TAG/apache/jk.conf -O /etc/apache2/mods-available/jk.conf
a2dissite 000-default.conf
a2ensite 00_ctsms_https.conf
a2ensite 00_ctsms_http.conf
a2enmod ssl
a2enmod rewrite

###deploy server certificate
mkdir /etc/apache2/ssl
HOST_NAME=$(hostname)
openssl req -x509 -nodes -days 365 -newkey rsa:2048 -keyout /etc/apache2/ssl/apache.key -subj "/C=AT/ST=Austria/L=Graz/O=phoenix/CN=$HOST_NAME" -out /etc/apache2/ssl/apache.crt
chmod 600 /etc/apache2/ssl/*
systemctl reload apache2