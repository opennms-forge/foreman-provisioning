import groovy.json.JsonSlurper
import groovy.transform.Field
import groovyx.gpars.GParsPool
import org.opennms.pris.model.*
import java.util.logging.Logger

@Field Logger logger = Logger.getLogger("foreman")
@Field String username = config.getString("username")
@Field String password = config.getString("password")
@Field String urlbase = config.getString("foremanapi")
@Field String hostgroupFilter = config.getString("hostgroupfilter")
@Field String requisitionName = config.getString("reqname")
@Field String hostLocation = config.getString("hostLocation")

@Field String url = urlbase + 'hosts?per_page=1000'
@Field String authentication = "${username}:${password}".bytes.encodeBase64().toString()

final Requisition requisition = new Requisition();
requisition.setForeignSource(requisitionName);

long startTime = System.currentTimeMillis();

def hostList = new JsonSlurper().parseText(request(url));

GParsPool.withPool {
    // parallel host query
    hostList.results.eachParallel { result ->
        requisition.getNodes().add(getNodeForHost(result))
    }
}

logger.info("DONE in " + (System.currentTimeMillis() - startTime) + " ms");

def valueOrNotSpecified(value) {
    // null/empty checker. returns "Not specified" if true
    return value != null && !value.toString().trim().isEmpty() ? value : "Not specified"
}

def request(String url) {
    // API connection
    long start = System.currentTimeMillis();
    def connection = url.toURL().openConnection()
    connection.addRequestProperty("Authorization", "Basic ${authentication}")
    connection.setRequestMethod("GET")
    connection.doOutput = false
    connection.connect()
    def text = connection.content.text;
    logger.info("Request " + url + " took: " + (System.currentTimeMillis() - start) + " ms");
    return text;
}

def getCatList(def aResult, String... names){
    // fills category list
    List<RequisitionCategory> cats = new ArrayList<>();
    for (def name in names){
        boolean isVirtual = name.equals("is_virtual");
        if(isVirtual && aResult.get(name).equals("true")) {
            name = "virtual";
            cats.add(new RequisitionCategory(valueOrNotSpecified(name)));
        } else if(isVirtual && aResult.get(name).equals("false")) {
            name = "physical";
            cats.add(new RequisitionCategory(valueOrNotSpecified(name)));
        } else {
            cats.add(new RequisitionCategory(valueOrNotSpecified(aResult.get(name))));
        }
    }
    return cats;
}

def getNodeForHost(result) {
    // fetch only nodes in defined hostgroup
    if (result.hostgroup_title != hostgroupFilter) {
        return;
    }
    // fetch only nodes in defined hostlocation
    if (result.location_name != hostLocation) {
        return;
    }

    logger.info('Starting with: ' + result.name);

    // Create a new requisition node
    RequisitionNode requisitionNode = new RequisitionNode()
    // Set node label and foreign ID for the node
    requisitionNode.setNodeLabel(result.name)
    requisitionNode.setForeignId(result.name)

    logger.info('Grabbing facts for: ' + result.name);

    def factlist = getFactsList(result.id)
    def aResult = factlist.results.get(result.name);

    // create interface
    RequisitionInterface requisitionInterface = new RequisitionInterface();
    def primaryFactIp = aResult.get("networking::primary")
    def ipFactString = 'networking::interfaces::' + primaryFactIp + '::ip'
    requisitionInterface.setIpAddr(aResult.get(ipFactString));
    requisitionInterface.setSnmpPrimary(PrimaryType.PRIMARY);
    requisitionNode.getInterfaces().add(requisitionInterface);

    // Create asset list
    List<RequisitionAsset> assetList = new ArrayList<RequisitionAsset>()
    RequisitionAsset assetCpu = new RequisitionAsset()
    RequisitionAsset assetRam = new RequisitionAsset()
    RequisitionAsset assetManufacturer = new RequisitionAsset()
    RequisitionAsset assetProductname = new RequisitionAsset()
    RequisitionAsset assetSerialnumber = new RequisitionAsset()
    RequisitionAsset assetOperatingSystem = new RequisitionAsset()
    RequisitionAsset assetDescription = new RequisitionAsset()

    if (aResult == null) {
        // Some nodes might not have facts. Generating a log entry
        logger.info("no facts for: " + result.name)
        return;
    }
    // fill asset fields. In case something is empty, "Not specified" will be set
    assetCpu.setName("cpu")
    assetCpu.setValue(valueOrNotSpecified(aResult.get("processor0")));

    assetRam.setName("ram")
    assetRam.setValue(valueOrNotSpecified(aResult.get("memorysize")));

    assetManufacturer.setName("manufacturer")
    assetManufacturer.setValue(valueOrNotSpecified(aResult.get("manufacturer")));

    assetProductname.setName("productname")
    assetProductname.setValue(valueOrNotSpecified(aResult.get("productname")));

    assetSerialnumber.setName("serialnumber")
    assetSerialnumber.setValue(valueOrNotSpecified(aResult.get("serialnumber")));

    assetDescription.setName("description")
    assetDescription.setValue(valueOrNotSpecified(aResult.get("proemion_app")))

    LsbDescription = valueOrNotSpecified(aResult.get("lsbdistdescription"))
    Kernelrelease = valueOrNotSpecified(aResult.get("kernelrelease"))

    assetOperatingSystem.setName("operatingsystem")
    assetOperatingSystem.setValue(LsbDescription + ' ' + Kernelrelease)

    // add assets to list
    assetList.add(assetCpu)
    assetList.add(assetRam)
    assetList.add(assetManufacturer)
    assetList.add(assetProductname)
    assetList.add(assetSerialnumber)
    assetList.add(assetOperatingSystem)
    assetList.add(assetDescription)
    requisitionNode.getAssets().addAll(assetList)

    // Grabs fact values and uses them for OpenNMS node categories.
    def catList = getCatList(aResult, "stack","umbworld", "is_virtual" );

    requisitionNode.getCategories().addAll(catList);
    return requisitionNode;
}

def getFactsList(id) {
    // Slurps the host facts into an array
    String facturl = urlbase + 'hosts/' + id + '/facts?per_page=1000'
    return new JsonSlurper().parseText(request(facturl));
}


return requisition;
