cp /ctsms/install/cron/ctsms /etc/cron.d/ctsms
chown root:root /etc/cron.d/ctsms
chmod 644 /etc/cron.d/ctsms
cp /ctsms/install/cron/my_department /etc/cron.d/my_department
chown root:root /etc/cron.d/my_department
chmod 644 /etc/cron.d/my_department
sed -r -i "s|-u cron -p 12345|-u my_department_cron -p ${CRON_PASSWORD}|" /etc/cron.d/my_department
systemctl restart cron