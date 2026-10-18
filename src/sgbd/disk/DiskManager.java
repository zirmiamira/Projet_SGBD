package sgbd.disk;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;

public class DiskManager implements IDiskManager {

    private static final String DATA_FILE_NAME = "data.bin";
    private static final String META_FILE_NAME = "dm.save";
    // file containing pages
    private RandomAccessFile dbFile;
    // file containing metadata
    private File metaFile;
    private int nbrpage;
    private int pageSize;
    private List<IPageId> availablePages;
    /**
     * The Constructor
     *
     */
    public DiskManager(){
        this.nbrpage = 0;
        this.availablePages = new ArrayList<>();
    }
   
    /**
     * Initializes the disk manager with a target working directory and a page size.
     *
     * @param dmDir    the absolute path to a valid working directory.
     * @param pageSize the size of a single page in bytes.
     * @throws DiskManagerException 
     */
    void Init(String dmDir, int pageSize) throws DiskManagerException;

    /**
     * Persists the current state and metadata of the disk manager
     * to the working directory.
     *
     * @throws DiskManagerException
     */
    void Save() throws DiskManagerException;

    /**
     * Allocates a new page requested by the upper layers of the DBMS.
     * @return the {@link IPageId} identifying the newly allocated page.
     * @throws DiskManagerException
     */
    IPageId AllocPage() throws DiskManagerException;

    /**
     * Reads the on-disk content of the page designated by {@code ipid}
     * into the provided buffer.
     * <p>
     * Note: The caller is responsible for allocating and passing the destination buffer.
     *
     * @param ipid   the identifier of the page to read.
     * @param buffer the buffer into which data will be copied
     * @throws DiskManagerException
     */
    void ReadPage(IPageId ipid, ByteBuffer buffer) throws DiskManagerException;

    /**
     * Writes the content of the provided buffer into the page designated by {@code ipid}.
     * This is the counterpart of {@link #ReadPage(IPageId, ByteBuffer)}.
     *
     * @param ipid   the identifier of the target page.
     * @param buffer the buffer containing the data to write.
     * @throws DiskManagerException
     */
    void WritePage(IPageId ipid, ByteBuffer buffer) throws DiskManagerException;

    /**
     * Deallocates the page identified by {@code ipid}, marking it as available
     * for future allocations.
     *
     * @param ipid the identifier of the page to free.
     * @throws DiskManagerException
     */
    void DeallocPage(IPageId ipid) throws DiskManagerException{
        if (ipid == null) {
            throw new DiskManagerException("l'ID de la page est null");
        }

        int pageNum = ((PageId) ipid).getPageId();

        if (pageNum == 0) {
            throw new DiskManagerException(" la Page 0  est réservée aux métadonnées");
        }

        if (pageNum < 0 || pageNum >= nbrpage) {
            throw new DiskManagerException("la page " + pageNum + " n'existe pas");
        }

        if (availablePages.contains(ipid)) {
            throw new DiskManagerException("la page " + pageNum + " est déjà libre ");
        }

availablePages.add(ipid);
    }

    /**
     * Returns the total number of pages currently in use (allocated and not yet deallocated).
     *
     * @return the count of active allocated pages.
     */
    int GetActivePageCount();

    /**
     * Returns the configured page size in bytes managed by this disk manager.
     *
     * @return the page size in bytes.
     */
    int GetPageSize();

}
