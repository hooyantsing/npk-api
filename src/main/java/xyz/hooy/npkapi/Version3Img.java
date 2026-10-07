package xyz.hooy.npkapi;

/**
 * IMG V3 格式实现 — 过渡版本（best-effort）。
 * <p>
 * V3 是极其罕见的 IMG 版本，格式资料推测其为开发调试期出现的过渡版本。
 * 截至本实现，<b>不存在任何公开的 V3 真实样本或第三方实现</b>
 * （Koishi 版本枚举、PyDnfEx、DNFToolKit、pacexy/npk-parser 均未收录 V3），
 * 故本类按最合理的假设实现：<b>沿用 V2 的完整文件布局，仅版本号字段为 3</b>
 * （过渡版本通常不改变容器布局）。
 * </p>
 * <p>
 * 注册进 {@link ImgFactory} 的意义：若真实 V3 文件出现，按 V2 布局 best-effort
 * 解析（读取时校验魔数与版本号，帧表/数据区解析失败会明确报损坏），
 * 而非因「版本未注册」直接拒绝整个 NPK 文件。
 * <b>本映射未经真实文件验证</b>；如获得样本，应据实际结构修正本类。
 * </p>
 *
 * @see Version2Img
 * @see AbstractImg
 */
public class Version3Img extends Version2Img {

    @Override
    public int getVersion() {
        return 3;
    }
}
