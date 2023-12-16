package xyz.hooy.npk.api.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;
import xyz.hooy.npk.api.constant.CompressModes;

/**
 * @author hooyantsing@gmail.com
 * @since 2023-05-04
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class TextureAttribute extends AbstractAttribute {

    private CompressModes compress = CompressModes.NON_ZLIB;
    private Integer width = 0;
    private Integer height = 0;
    private Integer x = 0;
    private Integer y = 0;
    private Integer frameWidth = 0;
    private Integer frameHeight = 0;
}
