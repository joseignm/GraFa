package cl.uchile.dcc.facet.web;

public class Entry {

    private String subject;
    private String label;
    private String description;
    private String altLabels;
    private String boosts;

    Entry(String subject, String label, String description, String altLabels, String boosts) {
        this.subject = subject;
        this.label = label;
        this.description = description;
        this.altLabels = altLabels;
        this.boosts = boosts;
    }

    public String getSubject() {
        return subject;
    }

    public String getLabel() {
        return label;
    }

    public String getAltLabels() {
        return altLabels;
    }

    public String getDescription() {
        return description;
    }

    public String getBoosts() { return boosts; }
}
