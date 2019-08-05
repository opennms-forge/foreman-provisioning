# Foreman Provisioning

The script can be used with PRIS script source to query Foremans API for host information.

# Details

* The script grabs hosts by hostgroups and put them into a requisition
* Only hosts with primary IP are selected
* The asset fields CPU, memory, manufacturer, productname, serialnumber, operatingsystem, description are filled with Puppet facts
* Service binding on interfaces is not possible since Foreman does not support it. So service discovery is required for each `foreignSource`

# Howto 

## PRIS Requisition Configuration

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

## OpenNMS 

* As already mentioned, specific service detectors for each requisition could make sense.
* Also some policies to set categories based on the requisition. For example: PRO, DEV, TEST or similar. So you can use it for filtering in notifications, available views etc.
* provisiond needs to be configured to get data from pris:
```
  <requisition-def import-name="REQUISITIONNAME" import-url-resource="http://PRIS:8000/requisitions/REQUISITIONNAME">
    <cron-schedule>* */5 * * * ? *</cron-schedule>
  </requisition-def>
``` 

# Known issues

If your Foreman uses HTTPS, make sure PRIS JVM is accepting the SSL certificate of the website.
Check out Ronnys [troubleshooting article](https://opennms.discourse.group/t/troubleshoot-java-with-self-signed-certificates/55).

# Example PRIS output


```xml
<node node-label="hostname.domain.com" foreign-id="422">
 <interface ip-addr="192.168.142.15" snmp-primary="P"/>
 <asset name="cpu" value="Intel(R) Xeon(R) Gold 5115 CPU @ 2.40GHz"/>
 <asset name="ram" value="64.00 GB"/>
 <asset name="manufacturer" value="Dell Inc."/>
 <asset name="productname" value="PowerEdge R740xd"/>
 <asset name="serialnumber" value="326PQQ2"/>
 <asset name="operatingsystem" value="Ubuntu 16.04 LTS 4.15.18-12-pve"/>
 <asset name="description" value="Not Specified"/>
</node>
```

# Additional code

In my case I'm using host and global parameters in Foreman.
They are used to provide something like tag informations.
In OpenNMS it makes sense to create catgories based on this information.
Since this code is too specific for general use, I've commented it out, but I didn't want to hide it completely.
If it makes sense for your environment, check out the additional code at the end of the script.