# Foreman Provisioning

The script can be used with PRIS script source to query Foremans API for host information.

# Details

* The script grabs hosts by hostgroups and location and put them into a requisition
* The asset fields CPU, memory, manufacturer, productname, serialnumber, operatingsystem, description are filled with Puppet facts
* Service binding on interfaces is not possible since Foreman does not have a logic for that. So service discovery is required for each `foreignSource` or the default `foreignSource`.

# Howto set it up

## PRIS Configuration

### Requisition file 

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
source.hostLocation=LOCATIONNAME

### default no-operation mapper
mapper = echo
```
### Libraries

The script requires some libraries which are not provided by default. Basically this is due to the fact, that the script runs the API calls in parallel instead of in a row to improve the performance.

These libs have to be stored in PRIS' lib folder:

gpars: https://mvnrepository.com/artifact/org.codehaus.gpars/gpars/1.2.1
ivy: https://mvnrepository.com/artifact/org.apache.ivy/ivy/2.4.0

## OpenNMS 

* As already mentioned, specific service detectors for each or default requisition could make sense.
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
<node node-label="node1.mydomain.com" foreign-id="node1.mydomain.com">
	<interface ip-addr="172.22.65.31" snmp-primary="P"/>
	<category name="it"/>
	<category name="pro"/>
	<category name="physical"/>
	<asset name="cpu" value="Intel(R) Xeon(R) Silver 4110 CPU @ 2.10GHz"/>
	<asset name="ram" value="30.90 GiB"/>
	<asset name="manufacturer" value="Dell Inc."/>
	<asset name="productname" value="PowerEdge R440"/>
	<asset name="serialnumber" value="8XHW2T2"/>
	<asset name="operatingsystem" value="Debian GNU/Linux 9.8 (stretch) 4.9.0-8-amd64"/>
	<asset name="description" value="database server"/>
</node>
<node node-label="node2.mydomain.com" foreign-id="node2.mydomain.com">
	<interface ip-addr="172.22.65.200" snmp-primary="P"/>
	<category name="it"/>
	<category name="pro"/>
	<category name="virtual"/>
	<asset name="cpu" value="Common KVM processor"/>
	<asset name="ram" value="481.30 MiB"/>
	<asset name="manufacturer" value="QEMU"/>
	<asset name="productname" value="Standard PC (i440FX + PIIX, 1996)"/>
	<asset name="serialnumber" value="Not specified"/>
	<asset name="operatingsystem" value="Ubuntu 18.04.3 LTS 4.15.0-64-generic"/>
	<asset name="description" value="foreman-proxy"/>
</node>
```