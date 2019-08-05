import groovy.json.JsonSlurper
import org.opennms.pris.model.*
import java.util.logging.Logger

// definitions filled out from requisition.properties
String username = config.getString("username")
String password = config.getString("password")
String urlbase = config.getString("foremanapi")
String hostgroupFilter = config.getString("hostgroupfilter") * /

Logger logger = Logger.getLogger("")

String per_page = "?per_page="
String url = urlbase + 'hosts' + per_page + '1000'
String authentication = "${username}:${password}".bytes.encodeBase64().toString()
connection = url.toURL().openConnection()
connection.addRequestProperty("Authorization", "Basic ${authentication}")
connection.setRequestMethod("GET")
connection.doOutput = false
connection.connect()

def hostlist = new JsonSlurper().parseText(connection.content.text)
int i = 0;
Requisition requisition = new Requisition()

// definition filled out from requisition.properties
def reqname = config.getString("reqname")
//create foreignSource
requisition.setForeignSource(reqname)

// for each host entry
for (result in hostlist.results) {

    // fetch correct nodes in hostgroup
    if (result.ip == null) {
        logger.info("no IP address set on: " + result.name);
        continue;
    }
    logger.info('Starting with: ' + result.name);
    // create a new requisition node
    RequisitionNode requisitionNode = new RequisitionNode()
    // Set node label and foreign ID for the node
    requisitionNode.setNodeLabel(result.name)
    requisitionNode.setForeignId(result.id.toString())
    // create interface
    RequisitionInterface requisitionInterface = new RequisitionInterface();
    requisitionInterface.setIpAddr(result.ip);
    requisitionInterface.setSnmpPrimary(PrimaryType.PRIMARY);
    requisitionNode.getInterfaces().add(requisitionInterface);
    // create facts string and catch data
    String facturl = urlbase + 'hosts/' + result.id + '/facts' + per_page + '1000'
    connectionfacts = facturl.toURL().openConnection()
    connectionfacts.addRequestProperty("Authorization", "Basic ${authentication}")
    connectionfacts.setRequestMethod("GET")
    connectionfacts.doOutput = false
    connectionfacts.connect()
    def factlist = new JsonSlurper().parseText(connectionfacts.content.text)
    // create asset list
    List < RequisitionAsset > assetList = new ArrayList < RequisitionAsset > ()
    RequisitionAsset assetCpu = new RequisitionAsset()
    RequisitionAsset assetRam = new RequisitionAsset()
    RequisitionAsset assetManufacturer = new RequisitionAsset()
    RequisitionAsset assetProductname = new RequisitionAsset()
    RequisitionAsset assetSerialnumber = new RequisitionAsset()
    RequisitionAsset assetOperatingSystem = new RequisitionAsset()
    RequisitionAsset assetDescription = new RequisitionAsset()

    // grabbing Puppet facts
    def stuff = factlist.results.get(result.name)
    logger.info('Grabbing facts for: ' + result.name);
    if (stuff == null) {
        logger.info("no facts for: " + result.name)
    } else {
        // define asset and write value
        assetCpu.setName("cpu")
        assetCpu.setValue(factlist.results.get(result.name).get("processor0"))
        assetRam.setName("ram")
        assetRam.setValue(factlist.results.get(result.name).get("memorysize"))
        assetManufacturer.setName("manufacturer")
        assetManufacturer.setValue(factlist.results.get(result.name).get("manufacturer"))
        assetProductname.setName("productname")
        assetProductname.setValue(factlist.results.get(result.name).get("productname"))
        assetSerialnumber.setName("serialnumber")
        assetSerialnumber.setValue(factlist.results.get(result.name).get("serialnumber"))
        assetLsbDescription = (factlist.results.get(result.name).get("lsbdistdescription"))
        assetKernelrelease = (factlist.results.get(result.name).get("kernelrelease"))
        assetDescription.setName("description")
        assetDescription.setValue(result.comment)
        assetOperatingSystem.setName("operatingsystem")
        assetOperatingSystem.setValue(assetLsbDescription + ' ' + assetKernelrelease)
        // add asset to list
        assetList.add(assetCpu)
        assetList.add(assetRam)
        assetList.add(assetManufacturer)
        assetList.add(assetProductname)
        if (assetSerialnumber.value == null) {
            logger.info("no serialnumber available: " + result.name);
            assetSerialnumber.setValue("Not Specified")
        }
        assetList.add(assetSerialnumber)
        assetList.add(assetOperatingSystem)
        if (assetDescription) {
            logger.info("no description available: " + result.name);
            assetDescription.setValue("Not Specified")
        }
        assetList.add(assetDescription)
        requisitionNode.getAssets().addAll(assetList)

    }
}

return requisition