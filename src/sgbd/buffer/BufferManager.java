package sgbd.buffer;
import java.nio.ByteBuffer;

import sgbd.common1.DBMSException;
import sgbd.disk.IDiskManager;
import sgbd.disk.IPageId;

public abstract class BufferManager implements IBufferManager {

    // Structure interne pour modéliser une case mémoire
    private class Frame {
        IPageId pageId = null;
        int pinCount = 0;
        boolean dirtyFlag = false;
        ByteBuffer buffer;

        public Frame(int pageSize) {
            this.buffer = ByteBuffer.allocate(pageSize);
        }
    }

    private int pageSize;
    private int bufferCount;
    private ReplacementPolicy policy;
    private IDiskManager diskManager;
    private Frame[] bufferPool;

    
    
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
	public void freePage(IPageId pageId, boolean valDirty) throws DBMSException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setPolicy(ReplacementPolicy policy) throws DBMSException {
		// TODO Auto-generated method stub
		
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
