package mobi.chouette.exchange.gtfs.model.importer;

import mobi.chouette.common.HTMLTagValidator;
import mobi.chouette.exchange.gtfs.importer.GtfsImportParameters;
import mobi.chouette.exchange.gtfs.model.GtfsAgency;
import mobi.chouette.exchange.gtfs.model.GtfsRoute;
import org.apache.commons.lang3.StringUtils;

import java.awt.*;
import java.io.IOException;
import java.text.Normalizer;
import java.util.Map;

public abstract class AbstractRouteById extends IndexImpl<GtfsRoute> implements GtfsConverter{

    public static enum FIELDS {
        route_id, agency_id, route_short_name, route_long_name, route_desc, route_type, route_url, route_color, route_text_color;
    };

    public static final String FILENAME = "routes.txt";
    public static final String KEY = RouteById.FIELDS.route_id.name();

    private GtfsRoute bean = new GtfsRoute();
    private String[] array = new String[RouteById.FIELDS.values().length];
    private String linePrefixToRemove;



    public AbstractRouteById(String name,String splitCharacter, String linePrefixToRemove) throws IOException {
        super(name, KEY,splitCharacter,linePrefixToRemove);
    }

    @Override
    protected void checkRequiredFields(Map<String, Integer> fields) {
        for (String fieldName : fields.keySet()) {
            if (fieldName != null) {
                if (!fieldName.equals(fieldName.trim())) {
                    // extra spaces in end fields are tolerated : 1-GTFS-CSV-7
                    // warning
                    getErrors().add(
                            new GtfsException(_path, 1, getIndex(fieldName), fieldName.trim(),
                                    GtfsException.ERROR.EXTRA_SPACE_IN_HEADER_FIELD, null, fieldName));
                }

                if (HTMLTagValidator.validate(fieldName.trim())) {
                    getErrors().add(
                            new GtfsException(_path, 1, getIndex(fieldName), fieldName.trim(),
                                    GtfsException.ERROR.HTML_TAG_IN_HEADER_FIELD, null, null));
                }

                boolean fieldNameIsExtra = true;
                for (RouteById.FIELDS field : RouteById.FIELDS.values()) {
                    if (fieldName.trim().equals(field.name())) {
                        fieldNameIsExtra = false;
                        break;
                    }
                }
                if (fieldNameIsExtra) {
                    // extra fields are tolerated : 1-GTFS-Route-11 warning
                    getErrors().add(
                            new GtfsException(_path, 1, getIndex(fieldName), fieldName,
                                    GtfsException.ERROR.EXTRA_HEADER_FIELD, null, null));
                }
            }
        }

        // no column agency_id
        // if (fields.get(FIELDS.agency_id.name()) == null) {
        // getErrors().add(new GtfsException(_path, 1, FIELDS.agency_id.name(),
        // GtfsException.ERROR.MISSING_REQUIRED_FIELDS, null, null));
        // }

        // checks for ubiquitous header fields : 1-GTFS-Stop-2 error
        if (fields.get(RouteById.FIELDS.route_id.name()) == null
                || (fields.get(RouteById.FIELDS.route_long_name.name()) == null && fields.get(RouteById.FIELDS.route_short_name.name()) == null)
                || fields.get(RouteById.FIELDS.route_type.name()) == null) {

            if (fields.get(RouteById.FIELDS.route_id.name()) == null) {
                throw new GtfsException(_path, 1, RouteById.FIELDS.route_id.name(), GtfsException.ERROR.MISSING_REQUIRED_FIELDS,
                        null, null);
            }

            if (fields.get(RouteById.FIELDS.route_long_name.name()) == null && fields.get(RouteById.FIELDS.route_short_name.name()) == null) {
                getErrors().add(
                        new GtfsException(_path, 1, RouteById.FIELDS.route_long_name.name(),
                                GtfsException.ERROR.MISSING_REQUIRED_FIELDS2, null, null));
            }

            if (fields.get(RouteById.FIELDS.route_type.name()) == null) {
                getErrors().add(
                        new GtfsException(_path, 1, RouteById.FIELDS.route_type.name(),
                                GtfsException.ERROR.MISSING_REQUIRED_FIELDS, null, null));
            }
        }
    }

    @Override
    protected GtfsRoute build(GtfsIterator reader, Context context) {
        int i = 0;
        for (RouteById.FIELDS field : RouteById.FIELDS.values()) {
            array[i++] = getField(reader, field.name());
        }

        i = 0;
        String value = null;
        int id = (int) context.get(Context.ID);
        clearBean();
        bean.setId(id);

        value = array[i++];
        testExtraSpace(RouteById.FIELDS.route_id.name(), value, bean);
        if (value != null && !value.trim().isEmpty()) {
            String rawRouteId = handleRouteIdTransformation(STRING_CONVERTER.from(context, FIELDS.route_id, value, "", true));
            String routeId = StringUtils.isNotEmpty(_linePrefixToRemove) ? rawRouteId.replaceFirst("^" + _linePrefixToRemove, "") : rawRouteId;
            bean.setRouteId(routeId);
        }

        value = array[i++];
        testExtraSpace(RouteById.FIELDS.agency_id.name(), value, bean);
        bean.setAgencyId(STRING_CONVERTER.from(context, RouteById.FIELDS.agency_id, value, GtfsAgency.DEFAULT_ID, false));

        boolean noShortName = false;
        value = array[i++];
        testExtraSpace(RouteById.FIELDS.route_short_name.name(), value, bean);
        if (value == null || value.trim().isEmpty()) {
            if (bean.getRouteId() != null){
                bean.setRouteShortName(bean.getRouteId());
            }
            noShortName = true;
            bean.getErrors().add(
                    new GtfsException(_path, id, getIndex(RouteById.FIELDS.route_short_name.name()),
                            RouteById.FIELDS.route_short_name.name(), GtfsException.ERROR.MISSING_ROUTE_SHORT_NAME, bean.getRouteId(),
                            value));
        } else {
            bean.setRouteShortName(STRING_CONVERTER.from(context, RouteById.FIELDS.route_short_name, value, false));
        }

        value = array[i++];
        testExtraSpace(RouteById.FIELDS.route_long_name.name(), value, bean);
        if (value == null || value.trim().isEmpty()) {
            if (noShortName)
                if (withValidation)
                    bean.getErrors().add(
                            new GtfsException(_path, id, getIndex(RouteById.FIELDS.route_long_name.name()),
                                    RouteById.FIELDS.route_long_name.name(), GtfsException.ERROR.MISSING_REQUIRED_VALUES2, null,
                                    null));
        } else {
            bean.setRouteLongName(STRING_CONVERTER.from(context, RouteById.FIELDS.route_long_name, value,
                    bean.getRouteShortName() != null));
            if (withValidation) {
                if (bean.getRouteLongName().equals(bean.getRouteShortName())) {
                    // routeshortName != routeLongName

                    bean.getErrors().add(
                            new GtfsException(_path, id, getIndex(RouteById.FIELDS.route_short_name.name()),
                                    RouteById.FIELDS.route_short_name.name() + "," + RouteById.FIELDS.route_long_name.name(),
                                    GtfsException.ERROR.SHARED_VALUE, bean.getRouteId(), bean.getRouteLongName()));
                } else if (!noShortName) { // ) &&
                    // bean.getRouteLongName().indexOf(bean.getRouteShortName())
                    // > 0)
                    // check routeshortName is not a substring of routeLongName
                    bean.getOkTests().add(GtfsException.ERROR.SHARED_VALUE);
                    String[] tokens = Normalizer.normalize(bean.getRouteLongName(), Normalizer.Form.NFD)
                            .replaceAll("[\u0300-\u036F]", "").split("[^A-Za-z0-9]");
                    boolean error = false;
                    String normalizedShortName = Normalizer.normalize(bean.getRouteShortName(), Normalizer.Form.NFD)
                            .replaceAll("[\u0300-\u036F]", "");
                    if (normalizedShortName.split("[^A-Za-z0-9]").length > 1) {
                        // short name contains special chars; check only from
                        // start or end
                        error = (bean.getRouteLongName().startsWith(bean.getRouteShortName()) || bean
                                .getRouteLongName().endsWith(bean.getRouteShortName()));
                    } else {
                        // check if one token is short name
                        for (String token : tokens) {
                            if (token.equals(normalizedShortName)) {
                                error = true;
                                break;
                            }
                        }
                    }
                    if (error)
                        bean.getErrors().add(
                                new GtfsException(_path, id, getIndex(RouteById.FIELDS.route_short_name.name()),
                                        RouteById.FIELDS.route_short_name.name(), GtfsException.ERROR.CONTAINS_ROUTE_NAMES, bean
                                        .getRouteId(), bean.getRouteShortName(), bean.getRouteLongName()));
                    else
                        bean.getOkTests().add(GtfsException.ERROR.CONTAINS_ROUTE_NAMES);

                } else {
                    bean.getOkTests().add(GtfsException.ERROR.SHARED_VALUE);
                    bean.getOkTests().add(GtfsException.ERROR.CONTAINS_ROUTE_NAMES);
                }
            }
        }

        value = array[i++];
        testExtraSpace(RouteById.FIELDS.route_desc.name(), value, bean);
        if (value != null && !value.trim().isEmpty()) {
            bean.setRouteDesc(STRING_CONVERTER.from(context, RouteById.FIELDS.route_desc, value, false));
            if (bean.getRouteDesc().equals(bean.getRouteLongName())) {
                if (withValidation)
                    bean.getErrors().add(
                            new GtfsException(_path, id, getIndex(RouteById.FIELDS.route_long_name.name()),
                                    RouteById.FIELDS.route_long_name.name() + "," + RouteById.FIELDS.route_desc.name(),
                                    GtfsException.ERROR.SHARED_VALUE, bean.getRouteId(), bean.getRouteLongName()));
            } else if (bean.getRouteDesc().equals(bean.getRouteShortName())) {
                if (withValidation)
                    bean.getErrors().add(
                            new GtfsException(_path, id, getIndex(RouteById.FIELDS.route_desc.name()), RouteById.FIELDS.route_short_name
                                    .name() + "," + RouteById.FIELDS.route_desc.name(), GtfsException.ERROR.SHARED_VALUE, bean
                                    .getRouteId(), bean.getRouteShortName()));
            }
        }

        value = array[i++];
        testExtraSpace(RouteById.FIELDS.route_type.name(), value, bean);
        if (value == null || value.trim().isEmpty()) {
            if (withValidation)
                bean.getErrors().add(
                        new GtfsException(_path, id, getIndex(RouteById.FIELDS.route_type.name()), RouteById.FIELDS.route_type.name(),
                                GtfsException.ERROR.MISSING_REQUIRED_VALUES, null, null));
        } else {
            try {
                bean.setRouteType(ROUTETYPE_CONVERTER.from(context, RouteById.FIELDS.route_type, value, true));
            } catch (GtfsException e) {
                if (withValidation)
                    bean.getErrors().add(
                            new GtfsException(_path, id, getIndex(RouteById.FIELDS.route_type.name()), RouteById.FIELDS.route_type.name(),
                                    GtfsException.ERROR.INVALID_FORMAT, null, value));
            }
        }

        value = array[i++];
        testExtraSpace(RouteById.FIELDS.route_url.name(), value, bean);
        if (value != null && !value.trim().isEmpty()) {
            try {
                bean.setRouteUrl(URL_CONVERTER.from(context, RouteById.FIELDS.route_url, value, false));
            } catch (GtfsException e) {
                // 1-GTFS-Route-7 warning
                if (withValidation)
                    bean.getErrors().add(
                            new GtfsException(_path, id, getIndex(RouteById.FIELDS.route_url.name()), RouteById.FIELDS.route_url.name(),
                                    GtfsException.ERROR.INVALID_FORMAT, null, value));
            }
        }

        value = array[i++];
        testExtraSpace(RouteById.FIELDS.route_color.name(), value, bean);
        try {
            bean.setRouteColor(COLOR_CONVERTER.from(context, RouteById.FIELDS.route_color, value, Color.WHITE, false));
        } catch (GtfsException e) {
            // 1-GTFS-Route-7 warning
            if (withValidation)
                bean.getErrors().add(
                        new GtfsException(_path, id, getIndex(RouteById.FIELDS.route_color.name()), RouteById.FIELDS.route_color.name(),
                                GtfsException.ERROR.INVALID_FORMAT, null, value));
        }
        if (bean.getRouteColor() == null)
            bean.setRouteColor(Color.WHITE);

        value = array[i++];
        testExtraSpace(RouteById.FIELDS.route_text_color.name(), value, bean);
        try {
            bean.setRouteTextColor(COLOR_CONVERTER.from(context, RouteById.FIELDS.route_text_color, value, Color.BLACK, false));
        } catch (GtfsException e) {
            // 1-GTFS-Route-7 warning
            if (withValidation)
                bean.getErrors().add(
                        new GtfsException(_path, id, getIndex(RouteById.FIELDS.route_color.name()), RouteById.FIELDS.route_color.name(),
                                GtfsException.ERROR.INVALID_FORMAT, null, value));
        }
        if (bean.getRouteTextColor() == null)
            bean.setRouteTextColor(Color.BLACK);

        float routeColorBrightness = (float) ((299 * bean.getRouteColor().getRed())
                + (587 * bean.getRouteColor().getGreen()) + (114 * bean.getRouteColor().getBlue()))
                / (float) 1000;
        float routeTextColorBrightness = (float) ((299 * bean.getRouteTextColor().getRed())
                + (587 * bean.getRouteTextColor().getGreen()) + (114 * bean.getRouteTextColor().getBlue()))
                / (float) 1000;
        float brightnessDifference = Math.max(routeColorBrightness, routeTextColorBrightness)
                - Math.min(routeColorBrightness, routeTextColorBrightness);
        int colorDifference = Math.max(bean.getRouteColor().getRed(), bean.getRouteTextColor().getRed())
                - Math.min(bean.getRouteColor().getRed(), bean.getRouteTextColor().getRed())
                + Math.max(bean.getRouteColor().getGreen(), bean.getRouteTextColor().getGreen())
                - Math.min(bean.getRouteColor().getGreen(), bean.getRouteTextColor().getGreen())
                + Math.max(bean.getRouteColor().getBlue(), bean.getRouteTextColor().getBlue())
                - Math.min(bean.getRouteColor().getBlue(), bean.getRouteTextColor().getBlue());
        if (brightnessDifference < 125 || colorDifference < 500) { // Poor
            // visibility
            // between
            // text and
            // background
            // colors
            if (getIndex(RouteById.FIELDS.route_text_color.name()) != null) {
                if (withValidation)
                    bean.getErrors().add(
                            new GtfsException(_path, id, getIndex(RouteById.FIELDS.route_text_color.name()),
                                    RouteById.FIELDS.route_text_color.name(), GtfsException.ERROR.BAD_COLOR, bean.getRouteId(),
                                    value));
            } else {
                if (withValidation)
                    bean.getErrors().add(
                            new GtfsException(_path, id, RouteById.FIELDS.route_text_color.name(), GtfsException.ERROR.BAD_COLOR,
                                    bean.getRouteId(), value));
            }
        }

        return bean;
    }

    @Override
    public boolean validate(GtfsRoute bean, GtfsImporter dao) {
        boolean result = true;

        GtfsRoute copy_bean = new GtfsRoute(bean);
        // Verify the agency_id
        String agencyId = copy_bean.getAgencyId();
        if (agencyId == null || GtfsAgency.DEFAULT_ID.equals(agencyId)) {
            if (dao.getAgencyById().getLength() == 1) {
                agencyId = dao.getAgencyById().iterator().next().getAgencyId();
            } else {
                agencyId = GtfsAgency.DEFAULT_ID;
            }
            copy_bean.setAgencyId(agencyId);
        }
        if (dao.getAgencyById().getValue(agencyId) == null) {
            // this bean has no agency
            if (getIndex(RouteById.FIELDS.agency_id.name()) == null)
                bean.getErrors().add(
                        new GtfsException(_path, copy_bean.getId(), RouteById.FIELDS.agency_id.name(),
                                GtfsException.ERROR.UNREFERENCED_ID, copy_bean.getRouteId(), agencyId));
            else
                bean.getErrors().add(
                        new GtfsException(_path, copy_bean.getId(), getIndex(RouteById.FIELDS.agency_id.name()), RouteById.FIELDS.agency_id
                                .name(), GtfsException.ERROR.UNREFERENCED_ID, copy_bean.getRouteId(), agencyId));
            result = false;
        }
        if (result)
            bean.getOkTests().add(GtfsException.ERROR.UNREFERENCED_ID);

        // routeUrl != GtfsAgency.agencyUrl
        boolean result2 = true;
        if (copy_bean.getRouteUrl() != null) {
            for (GtfsAgency agency : (AgencyById) dao.getAgencyById()) {
                if (agency.getAgencyUrl() != null) {
                    if (copy_bean.getRouteUrl().equals(agency.getAgencyUrl())) {
                        result2 = false;
                        bean.getErrors().add(
                                new GtfsException(_path, copy_bean.getId(), getIndex(RouteById.FIELDS.route_url.name()),
                                        RouteById.FIELDS.route_url.name() + "," + AgencyById.FIELDS.agency_url.name(),
                                        GtfsException.ERROR.SHARED_VALUE, copy_bean.getRouteId(), copy_bean
                                        .getRouteUrl().toString()));
                        break;
                    }
                }
            }
        }
        if (result2)
            bean.getOkTests().add(GtfsException.ERROR.SHARED_VALUE);
        result = result && result2;

        // (routeShortName, routeLongName) != other GtfsRoute.(routeShortName,
        // routeLongName)
//		boolean result3 = true;
        // (routeShortName, routeLongName) != other GtfsRoute.(routeShortName,
        // routeLongName)
//		boolean result4 = true;
//		String routeShortName = copy_bean.getRouteShortName();
//		String routeLongName = copy_bean.getRouteLongName();
//		for (GtfsRoute another_bean : dao.getRouteById()) {
//			if (another_bean.getId() >= copy_bean.getId())
//				break;
//			if ((routeShortName != null && routeShortName.equals(another_bean.getRouteShortName()))
//					|| (routeShortName == another_bean.getRouteShortName()))
//				if ((routeLongName != null && routeLongName.equals(another_bean.getRouteLongName()))
//						|| (routeLongName == another_bean.getRouteLongName())) {
//					bean.getErrors().add(
//							new GtfsException(, copy_bean.getId(), getIndex(FIELDS.route_short_name.name()),
//									FIELDS.route_short_name.name(), GtfsException.ERROR.DUPLICATE_ROUTE_NAMES,
//									copy_bean.getRouteId(), another_bean.getRouteId()));
//					result3 = false;
//				}
//			if ((routeShortName != null && routeShortName.equals(another_bean.getRouteLongName()))
//					|| (routeShortName == another_bean.getRouteLongName()))
//				if ((routeLongName != null && routeLongName.equals(another_bean.getRouteShortName()))
//						|| (routeLongName == another_bean.getRouteShortName())) {
//					bean.getErrors().add(
//							new GtfsException(_path, copy_bean.getId(), getIndex(FIELDS.route_long_name.name()),
//									FIELDS.route_long_name.name(), GtfsException.ERROR.INVERSE_DUPLICATE_ROUTE_NAMES,
//									copy_bean.getRouteId(), another_bean.getRouteId()));
//					result4 = false;
//				}
//		}
//		if (result3)
//			bean.getOkTests().add(GtfsException.ERROR.DUPLICATE_ROUTE_NAMES);
//		if (result4)
//			bean.getOkTests().add(GtfsException.ERROR.INVERSE_DUPLICATE_ROUTE_NAMES);
// 		result = result && result3 && result4;

        return result;
    }

    /**
     * Apply a transformation on routeId
     * @param inputRouteId
     * @return The routeId transformed
     */
    protected abstract String handleRouteIdTransformation(String inputRouteId);

    private void clearBean() {
        // bean.getErrors().clear();
        bean.setId(null);
        bean.setAgencyId(null);
        bean.setRouteColor(null);
        bean.setRouteDesc(null);
        bean.setRouteId(null);
        bean.setRouteLongName(null);
        bean.setRouteShortName(null);
        bean.setRouteTextColor(null);
        bean.setRouteType(null);
        bean.setRouteUrl(null);
    }

}
