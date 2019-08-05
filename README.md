# Foreman Provisioning

The script can be used with PRIS script source to query Foremans API for host information.

# Details

* The script grabs hosts by hostgroups and put them into a requisition
* Only hosts with primary IP are selected
* The asset fields CPU, memory, manufacturer, productname, serialnumber, operatingsystem, description are filled with Puppet facts
* Service binding on interfaces is not possible since Foreman does not support it. So service discovery is required for each `foreignSource`

# PRIS Requisition Configuration

The requisition config is quite simple.
The variable values will be used in the script.


```
source = script
source.file = path/to/foreman.groovy

source.foremanapi=https://myForeman.domain/api/
source.username=USER
source.password=PASSWORD
source.hostgroupfilter=HOSTGROUP
source.reqname=REQUISITIONNAME

### default no-operation mapper
mapper = echo
```

# Known issues

If your Foreman uses HTTPS, make sure PRIS JVM is accepting the SSL certificate of the website.
Check out Ronnys [troubleshooting article](https://opennms.discourse.group/t/troubleshoot-java-with-self-signed-certificates/55).