package xyz.hooy.npk.api.entity;

/**
 * @author hooyantsing@gmail.com
 * @since 2023-05-04
 */
public abstract class AbstractIndexAttribute {

    protected Integer type;

    public Integer getType() {
        return type;
    }

    public void setType(Integer type) {
        this.type = type;
    }
}
