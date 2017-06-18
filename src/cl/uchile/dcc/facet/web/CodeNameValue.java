package cl.uchile.dcc.facet.web;

public class CodeNameValue {
    private String code;
    private String name;
    private int value;

    CodeNameValue(String code, String name, int value) {
        this.code = code;
        this.name = name;
        this.value = value;
    }

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public int getValue() {
        return value;
    }

}
