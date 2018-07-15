package org.lamikvah.website.data;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public enum Plan {

    STANDARD("standard-1", "$360"),

    SILVER("silver-1", "$500"),

    GOLD("gold-1", "$1,000"),

    CHAI("platinum-1", "$1,800");

    private static final Map<String, Plan> ID_TO_PLAN_MAP = new HashMap<>();

    static {
        for(Plan plan: values()) {
            Plan previousValue = ID_TO_PLAN_MAP.put(plan.getStripePlanId(), plan);
            if(previousValue != null) {
                throw new Error("Duplicate plan ID " + plan.getStripePlanId() + " for " + previousValue.name() + " and " + plan.name());
            }
        }
    }

    private final String stripePlanId;
    private final String formattedPrice;

    private Plan(String stripePlanId, String formattedPrice) {
        this.stripePlanId = stripePlanId;
        this.formattedPrice = formattedPrice;
    }

    public String getStripePlanId() {
        return stripePlanId;
    }

    public String getFormattedPrice() {
        return formattedPrice;
    }

    public static Optional<Plan> forStripePlanName(String id) {

        return Optional.ofNullable(ID_TO_PLAN_MAP.get(id));

    }
}
