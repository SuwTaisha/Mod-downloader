import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;

/** Carries a ModrinthSearchHit by reference for in-app drag-and-drop (browse list -> install list). */
final class ModHitTransferable implements Transferable {

    static final DataFlavor FLAVOR = new DataFlavor(ModrinthSearchHit.class, "ModrinthSearchHit");

    private final ModrinthSearchHit hit;

    ModHitTransferable(ModrinthSearchHit hit) {
        this.hit = hit;
    }

    @Override
    public DataFlavor[] getTransferDataFlavors() {
        return new DataFlavor[]{FLAVOR};
    }

    @Override
    public boolean isDataFlavorSupported(DataFlavor flavor) {
        return FLAVOR.equals(flavor);
    }

    @Override
    public Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException {
        if (!isDataFlavorSupported(flavor)) {
            throw new UnsupportedFlavorException(flavor);
        }
        return hit;
    }
}
