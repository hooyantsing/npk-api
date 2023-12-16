package xyz.hooy.npk.api.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @author hooyantsing@gmail.com
 * @since 2023-05-04
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class LinkAttribute extends AbstractAttribute {

    private Integer to;
}
