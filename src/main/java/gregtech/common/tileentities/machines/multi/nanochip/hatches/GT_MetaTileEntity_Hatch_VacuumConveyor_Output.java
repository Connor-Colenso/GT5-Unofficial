package gregtech.common.tileentities.machines.multi.nanochip.hatches;

import net.minecraftforge.common.util.ForgeDirection;

import com.github.technus.tectech.thing.metaTileEntity.pipe.GT_MetaTileEntity_Pipe_Data;
import com.github.technus.tectech.util.TT_Utility;

import gregtech.api.interfaces.ITexture;
import gregtech.api.interfaces.metatileentity.IMetaTileEntity;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import gregtech.api.metatileentity.MetaTileEntity;
import gregtech.common.tileentities.machines.multi.nanochip.util.IConnectsToVacuumConveyor;

public class GT_MetaTileEntity_Hatch_VacuumConveyor_Output extends GT_MetaTileEntity_Hatch_VacuumConveyor {

    public GT_MetaTileEntity_Hatch_VacuumConveyor_Output(int aID, String aName, String aNameRegional, int aTier) {
        super(aID, aName, aNameRegional, aTier, new String[] {});
        TT_Utility.setTier(aTier, this);
    }

    public GT_MetaTileEntity_Hatch_VacuumConveyor_Output(String aName, int aTier, String[] aDescription,
        ITexture[][][] aTextures) {
        super(aName, aTier, aDescription, aTextures);
    }

    @Override
    public MetaTileEntity newMetaEntity(IGregTechTileEntity aTileEntity) {
        return new GT_MetaTileEntity_Hatch_VacuumConveyor_Output(mName, mTier, mDescriptionArray, mTextures);
    }

    @Override
    public boolean isOutputFacing(ForgeDirection side) {
        return side == getBaseMetaTileEntity().getFrontFacing();
    }

    @Override
    public boolean isInputFacing(ForgeDirection side) {
        return false;
    }

    @Override
    public boolean isSimpleMachine() {
        return true;
    }

    @Override
    public boolean isComponentsInputFacing(ForgeDirection side) {
        return isInputFacing(side);
    }

    @Override
    public void moveAround(IGregTechTileEntity aBaseMetaTileEntity) {
        IConnectsToVacuumConveyor current = this, source = this, next;
        int range = 0;
        while ((next = current.getNext(source)) != null && range++ < 1000) {
            if (next instanceof GT_MetaTileEntity_Hatch_VacuumConveyor_Input) {
                ((GT_MetaTileEntity_Hatch_VacuumConveyor_Input) next).setContents(contents);
                break;
            }
            source = current;
            current = next;
        }
        contents = null;
    }

    @Override
    public IConnectsToVacuumConveyor getNext(IConnectsToVacuumConveyor source) {
        IGregTechTileEntity base = getBaseMetaTileEntity();
        byte color = base.getColorization();
        if (color < 0) {
            return null;
        }
        IGregTechTileEntity next = base.getIGregTechTileEntityAtSide(base.getFrontFacing());
        if (next == null) {
            return null;
        }
        IMetaTileEntity meta = next.getMetaTileEntity();
        // TODO: Use correct pipe type
        if (meta instanceof GT_MetaTileEntity_Pipe_Data) {
            ((GT_MetaTileEntity_Pipe_Data) meta).markUsed();
            return (IConnectsToVacuumConveyor) meta;
        } else if (meta instanceof GT_MetaTileEntity_Hatch_VacuumConveyor_Input
            && ((GT_MetaTileEntity_Hatch_VacuumConveyor_Input) meta).getColorization() == color
            && ((GT_MetaTileEntity_Hatch_VacuumConveyor_Input) meta).canConnect(
                base.getFrontFacing()
                    .getOpposite())) {
                        return (IConnectsToVacuumConveyor) meta;
                    }
        return null;
    }

    @Override
    public boolean canConnect(ForgeDirection side) {
        return isOutputFacing(side);
    }
}
