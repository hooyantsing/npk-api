package xyz.hooy.npkapi.npk.constant;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum AlbumSuffixModes {

    IMAGE("img"), AUDIO("ogg");

    private final String suffix;
}
