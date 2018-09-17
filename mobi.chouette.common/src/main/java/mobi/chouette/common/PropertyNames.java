package mobi.chouette.common;

public interface PropertyNames {
    String ROOT_DIRECTORY = ".directory";
    String ADMIN_KEY = ".admin.key";
    String MAX_STARTED_JOBS = ".started.jobs.max";
	String JOB_SHCEDULE_INTERVAL_MS = ".jobs.schedule.interval.ms";
    String MAX_STARTED_TRANSFER_JOBS = ".started.transfer.jobs.max";
    String MAX_COPY_BY_JOB = ".copy.by.import.max";

    String RESCHEDULE_INTERRUPTED_JOBS = ".reschedule.interrupted.jobs";

    /** Whether ids are mapped using external stop place registry during import.
     * Disabling this will cause no ids to be mapped, regardless of input param. */
    String STOP_PLACE_ID_MAPPING = ".stop.place.id.mapping";
    String STOP_PLACE_REGISTER_UPDATE = ".stop.place.register.update";
    String STOP_PLACE_REGISTER_URL = ".stop.place.register.update.url";

    String REFERENTIAL_LOCK_MANAGER_IMPLEMENTATION = ".referential.lock.manager.impl";
    String KUBERNETES_ENABLED = ".kubernetes.enabled";
	String FILE_STORE_IMPLEMENTATION = ".file.store.impl";
    String GTFS_AGENCY_URL_DEFAULTS = "iev.gtfs.agency.url.defaults";
    String GTFS_AGENCY_PHONE_DEFAULTS = "iev.gtfs.agency.phone.defaults";
    String OSRM_ROUTE_SECTIONS_BASE = "iev.osrm.endpoint.";

    String KC_CLIENT_ID = ".keycloak.resource";
    String KC_CLIENT_SECRET = ".iam.keycloak.client.secret";
    String KC_CLIENT_REALM = ".keycloak.realm";
    String KC_CLIENT_AUTH_URL = ".keycloak.auth-server-url";


}