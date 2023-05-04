package xyz.hooy.npk.api.model;

public class Reference extends Index {

    private final ReferenceAttribute referenceAttribute = new ReferenceAttribute();

    public ReferenceAttribute getReferenceAttribute() {
        return referenceAttribute;
    }
}
