package xyz.hooy.npkapi.component;

import lombok.Getter;

import javax.imageio.metadata.IIOInvalidTreeException;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.metadata.IIOMetadataNode;

public class IIOMetadataExpansion {

    private final IIOMetadata metadata;
    private final String metaFormatName;
    private final IIOMetadataNode root;
    private final IIOMetadataNode graphicsControlExtensionNode;

    @Getter
    public enum DisposalMethod{

        NONE("none"),
        DO_NOT_DISPOSE("disNotDispose"),
        RESTORE_TO_BACKGROUND_COLOR("restoreToBackgroundColor"),
        RESTORE_TO_PREVIOUS("restoreToPrevious");

        private final String value;

        DisposalMethod(String value){
            this.value = value;
        }
    }

    public IIOMetadataExpansion(IIOMetadata metadata) {
        this.metadata = metadata;
        this.metaFormatName = metadata.getNativeMetadataFormatName();
        this.root = (IIOMetadataNode) metadata.getAsTree(metaFormatName);
        this.graphicsControlExtensionNode = getNode(root, "GraphicControlExtension");
    }

    public IIOMetadataExpansion setDisposalMethod(DisposalMethod method) {
        graphicsControlExtensionNode.setAttribute("disposalMethod", method.getValue());
        return this;
    }

    public IIOMetadataExpansion setUserInputFlag(boolean flag) {
        graphicsControlExtensionNode.setAttribute("userInputFlag", String.valueOf(flag));
        return this;
    }

    public IIOMetadataExpansion setTransparentColorFlag(boolean flag) {
        graphicsControlExtensionNode.setAttribute("transparentColorFlag", String.valueOf(flag));
        return this;
    }

    public IIOMetadataExpansion setDelayTime(long delayTIme) {
        graphicsControlExtensionNode.setAttribute("delayTime", String.valueOf(delayTIme));
        return this;
    }

    public IIOMetadataExpansion setTransparentColorIndex(int index) {
        graphicsControlExtensionNode.setAttribute("transparentColorIndex", String.valueOf(index));
        return this;
    }

    public IIOMetadata apply() throws IIOInvalidTreeException {
        metadata.setFromTree(metaFormatName, root);
        return metadata;
    }

    private IIOMetadataNode getNode(IIOMetadataNode rootNode, String nodeName) {
        int nNodes = rootNode.getLength();
        for (int i = 0; i < nNodes; i++) {
            if (rootNode.item(i).getNodeName().equalsIgnoreCase(nodeName)) {
                return (IIOMetadataNode) rootNode.item(i);
            }
        }
        IIOMetadataNode node = new IIOMetadataNode(nodeName);
        rootNode.appendChild(node);
        return (node);
    }
}
