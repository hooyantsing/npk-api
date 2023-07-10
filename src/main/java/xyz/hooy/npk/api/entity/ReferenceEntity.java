package xyz.hooy.npk.api.entity;

/**
 * @author hooyantsing@gmail.com
 * @since 2023-05-04
 */
public class ReferenceEntity extends AbstractIndex {

    private final ReferenceAttribute referenceAttribute = new ReferenceAttribute();

    public ReferenceAttribute getReferenceAttribute() {
        return referenceAttribute;
    }
}
