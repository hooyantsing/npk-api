package xyz.hooy.npk.api.entity;

import lombok.Data;
import xyz.hooy.npk.api.constant.ColorLinkTypes;

/**
 * @author hooyantsing@gmail.com
 * @since 2023-05-04
 */
@Data
public abstract class AbstractAttribute {

    protected ColorLinkTypes type;
}
