# npk-api

> 提供对《地下城与勇士》（DNF、DFO）游戏资源 NPK 文件基础操作能力。

[![Java](https://img.shields.io/badge/java-8+-ae7118.svg?style=flat-square)](https://www.oracle.com/cn/java/technologies)

### 平台特性

* 依赖极少第三方库，规避潜在的依赖冲突；
* 简洁易用的 API，对 NPK 及其包含 IMG 文件增、删、改、查等基础能力；
* 可在内存中直接读取素材，或将其导出 PNG、GIF 等格式文件。

### 计划清单

* 目前仅支持 IMGV2 格式，未来将支持更多，如 IMGV4 等；
* 导出 GIF 等文件；
* 兼容 FXGL 游戏引擎。

### 使用说明

详见 `src/test/java/**` 的测试用例。

### 许可协议

[暂无]()

### 参考资料

1. [NPK解包器使用演示及原理讲解](https://www.bilibili.com/video/BV1S8411873f/?spm_id_from=333.337.search-card.all.click&vd_source=2ddde2ac0a5860b504d84ead79ea843c)
2. [关于DNF的多媒体包NPK文件的那些事儿](https://www.php1.cn/detail/GuanYu_DNF_DeDuo_138a19ad.html)
3. [langresser-dnfextrator](https://github.com/langresser/dnfextrator/blob/master/extradnf/extradnf/extradnf.cpp)
