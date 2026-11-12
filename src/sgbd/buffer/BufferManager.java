package sgbd.buffer;
import java.nio.ByteBuffer;

import sgbd.common.DBMSException;
import sgbd.disk.IDiskManager;
import sgbd.disk.IPageId;

public abstract class BufferManager implements IBufferManager {

	  /**
	 * Représente une case mémoire du Buffer Pool.
	 * Chaque Frame contient une page chargée depuis le disque ainsi que
	 * les informations nécessaires à sa gestion et à son remplacement.
	 */
	private class Frame {
	    /*Identifiant de la page actuellement stockée dans cette frame*/
	    IPageId pageId = null;
	    /** Nombre de fois où la page est actuellement utilisée*/
	    int pinCount = 0;
	    /*Indique si la page a été modifiée en mémoire et doit être sauvegardée sur disque.*/
	    boolean dirtyFlag = false;
	    /* Buffer mémoire contenant les données de la page. */
	    ByteBuffer buffer;
	    /* Instant du dernier passage du pinCount à 0.*/
	    private long t = 0;
	    /**
	     * Construit une frame avec un buffer mémoire de la taille d'une page.
	     * @param pageSize Taille du buffer en octets.
	     */
	    public Frame(int pageSize) {
	        this.buffer = ByteBuffer.allocate(pageSize);
	    }
	}
	
    private int pageSize;
    private int bufferCount;
    private ReplacementPolicy policy;
    private IDiskManager diskManager;
    private Frame[] bufferPool;
	private boolean isInitialized = false;

    
    
    @Override
    public void init(int pageSize, int bufferCount, ReplacementPolicy policy, IDiskManager diskManager) throws DBMSException {
        this.pageSize = pageSize;
        this.bufferCount = bufferCount;
        this.policy = policy;
        this.diskManager = diskManager;
        
        this.bufferPool = new Frame[bufferCount];
        for (int i = 0; i < bufferCount; i++) {
            this.bufferPool[i] = new Frame(pageSize);
        }
    }
    
    
    

    public ByteBuffer getPage(IPageId pageId) throws DBMSException {
        
        // Étape 1 : Vérifier si la page est déjà en RAM
        for (Frame frame : bufferPool) {
            if (frame.pageId != null && frame.pageId.equals(pageId)) {
                frame.pinCount++;
                return frame.buffer;
            }
        }

        // Étape 2 : Trouver une case libre
        Frame targetFrame = null;
        for (Frame frame : bufferPool) {
            if (frame.pinCount == 0) {
                targetFrame = frame;
                break; 
            }
        }

        if (targetFrame == null) {
            throw new BufferManagerException("Plus de place en mémoire, et politique de remplacement non implémentée.");
        }

        // Étape 3 : Gérer la sauvegarde (Dirty) de l'ancienne page
        if (targetFrame.pageId != null && targetFrame.dirtyFlag) {
            diskManager.WritePage(targetFrame.pageId, targetFrame.buffer);
        }

        // Étape 4 : Charger la nouvelle page
        targetFrame.pageId = pageId;
        targetFrame.pinCount = 1;
        targetFrame.dirtyFlag = false;
        
        diskManager.ReadPage(pageId, targetFrame.buffer);
        
        return targetFrame.buffer;
    }
    
        

	@Override
	 /**
     * Releases a previously requested page.
     *
     * @param pageId   The identifier of the page to free.
     * @param valDirty Whether the page content has been modified.
     * @throws DBMSException
     */
	public void freePage(IPageId pageId, boolean valDirty) throws DBMSException {
	
	    // Vérifier l'initialisation
	    if (!isInitialized) {
	        throw new BufferManagerException("Le BufferManager doit être initialisé avant toute utilisation.");
	    }
	    // Vérifier l'identifiant
	    if (pageId == null) {
	        throw new BufferManagerException("L'identifiant de page ne peut pas être null.");
	    }
	    // Rechercher la page dans le BufferPool
	    for (Frame frame : bufferPool) {
	
	        if (frame.pageId != null && frame.pageId.equals(pageId)) {
	            // Vérifier que la page est actuellement épinglée
	            if (frame.pinCount <= 0) {
	                throw new BufferManagerException("Impossible de libérer la page : elle est déjà libre.");
	            }
	            // Mettre à jour le dirty flag
	            if (valDirty) {
	                frame.dirtyFlag = true;
	            }
	            // Décrémenter le pin count
	            frame.pinCount--;
	            if (frame.pinCount == 0) {
	                frame.t = System.nanoTime();
	            }
	            return;
	        }
	    }
	    // La page n'existe pas dans le BufferPool
	    throw new BufferManagerException("Impossible de libérer la page : elle n'est pas présente dans le BufferPool.");
	}
	
	@Override
    /**
     * Updates the current replacement policy.
     * Overrides the policy previously configured.
     *
     * @param policy The new replacement policy to use.
     * @throws DBMSException
     */
	public void setPolicy(ReplacementPolicy policy) throws DBMSException {
	    if (!isInitialized) {
	        throw new BufferManagerException("BufferManager is not initialized.");
	    }

	    if (policy == null) {
	        throw new BufferManagerException("Replacement policy cannot be null.");
	    }

	    this.policy = policy;
	}

	
	@Override
	public void saveBuffers() throws DBMSException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public int getPinCount(IPageId pageId) throws DBMSException {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public boolean getDirtyFlag(IPageId pageId) throws DBMSException {
		// TODO Auto-generated method stub
		return false;
	}
}
