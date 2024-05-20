package gregtech.common.tileentities.machines.multi.purification;

import static com.gtnewhorizon.structurelib.structure.StructureUtility.lazy;
import static com.gtnewhorizon.structurelib.structure.StructureUtility.ofBlock;
import static com.gtnewhorizon.structurelib.structure.StructureUtility.ofBlockAnyMeta;
import static com.gtnewhorizon.structurelib.structure.StructureUtility.ofChain;
import static gregtech.api.enums.GT_HatchElement.InputBus;
import static gregtech.api.enums.GT_HatchElement.InputHatch;
import static gregtech.api.enums.GT_HatchElement.OutputBus;
import static gregtech.api.enums.GT_HatchElement.OutputHatch;
import static gregtech.api.enums.GT_Values.AuthorNotAPenguin;
import static gregtech.api.enums.Textures.BlockIcons.OVERLAY_FRONT_LARGE_CHEMICAL_REACTOR;
import static gregtech.api.enums.Textures.BlockIcons.OVERLAY_FRONT_LARGE_CHEMICAL_REACTOR_ACTIVE;
import static gregtech.api.enums.Textures.BlockIcons.OVERLAY_FRONT_LARGE_CHEMICAL_REACTOR_ACTIVE_GLOW;
import static gregtech.api.enums.Textures.BlockIcons.OVERLAY_FRONT_LARGE_CHEMICAL_REACTOR_GLOW;
import static gregtech.api.util.GT_StructureUtility.ofFrame;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.util.ForgeDirection;
import net.minecraftforge.fluids.FluidStack;

import org.jetbrains.annotations.NotNull;

import com.google.common.collect.ImmutableList;
import com.gtnewhorizon.structurelib.alignment.IAlignmentLimits;
import com.gtnewhorizon.structurelib.alignment.constructable.ISurvivalConstructable;
import com.gtnewhorizon.structurelib.structure.IStructureDefinition;
import com.gtnewhorizon.structurelib.structure.ISurvivalBuildEnvironment;
import com.gtnewhorizon.structurelib.structure.StructureDefinition;

import gregtech.api.GregTech_API;
import gregtech.api.enums.Materials;
import gregtech.api.enums.SoundResource;
import gregtech.api.enums.Textures;
import gregtech.api.enums.TierEU;
import gregtech.api.interfaces.IHatchElement;
import gregtech.api.interfaces.ITexture;
import gregtech.api.interfaces.metatileentity.IMetaTileEntity;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import gregtech.api.recipe.RecipeMap;
import gregtech.api.recipe.RecipeMaps;
import gregtech.api.recipe.check.CheckRecipeResult;
import gregtech.api.recipe.check.CheckRecipeResultRegistry;
import gregtech.api.render.TextureFactory;
import gregtech.api.util.GT_Multiblock_Tooltip_Builder;
import gregtech.api.util.GT_Recipe;
import gregtech.api.util.GT_StructureUtility;
import gregtech.api.util.GT_Utility;

public class GT_MetaTileEntity_PurificationUnitFlocculation
    extends GT_MetaTileEntity_PurificationUnitBase<GT_MetaTileEntity_PurificationUnitFlocculation>
    implements ISurvivalConstructable {

    private static final String STRUCTURE_PIECE_MAIN = "main";
    private static final String STRUCTURE_PIECE_MAIN_SURVIVAL = "main_survival";

    private static final int STRUCTURE_X_OFFSET = 4;
    private static final int STRUCTURE_Y_OFFSET = 3;
    private static final int STRUCTURE_Z_OFFSET = 0;

    public static final long INPUT_CHEMICAL_PER_LEVEL = 100000;
    private static final long WASTE_WATER_PER_LEVEL = INPUT_CHEMICAL_PER_LEVEL;
    public static final float SUCCESS_PER_LEVEL = 10.0f;

    private static final int CONSUME_INTERVAL = 20;

    private static final Materials INPUT_CHEMICAL = Materials.PolyAluminiumChloride;
    private static final Materials OUTPUT_WASTE = Materials.FlocculationWasteLiquid;

    private long inputFluidConsumed = 0;

    private static final String[][] structure = new String[][]
    // spotless:off
        {
        { "         ", "         ", " BBBBBBB ", " BBB~BBB ", " BBBBBBB " },
        { "         ", "         ", " B     B ", " BWWWWWB ", " BCCCCCB " },
        { "         ", "         ", " B     B ", " GWWWWWG ", " BCAAACB " },
        { "         ", "         ", " B     B ", " GWWWWWG ", " BCAAACB " },
        { "         ", "         ", " B     B ", " GWWWWWG ", " BCAAACB " },
        { "         ", " EE   EE ", " BE   EB ", " BEWWWEB ", " BCCCCCB " },
        { "D       D", "DEE   EED", "DBBBBBBBD", "DBBBBBBBD", "DBBBBBBBD" },
        { "DD     DD", "DD     DD", "DD     DD", "DD     DD", "DD     DD" }
        };
        // spotless:on

    private static final int MAIN_CASING_INDEX = getTextureIndex(GregTech_API.sBlockCasings9, 5);

    private static final IStructureDefinition<GT_MetaTileEntity_PurificationUnitFlocculation> STRUCTURE_DEFINITION = StructureDefinition
        .<GT_MetaTileEntity_PurificationUnitFlocculation>builder()
        .addShape(STRUCTURE_PIECE_MAIN, structure)
        .addShape(
            STRUCTURE_PIECE_MAIN_SURVIVAL,
            Arrays.stream(structure)
                .map(
                    sa -> Arrays.stream(sa)
                        .map(s -> s.replaceAll("W", " "))
                        .toArray(String[]::new))
                .toArray(String[][]::new))
        // Filter machine casing
        .addElement('A', ofBlock(GregTech_API.sBlockCasings3, 11))
        .addElement(
            'B',
            ofChain(
                lazy(
                    t -> GT_StructureUtility.<GT_MetaTileEntity_PurificationUnitFlocculation>buildHatchAdder()
                        .atLeastList(t.getAllowedHatches())
                        .casingIndex(MAIN_CASING_INDEX)
                        .dot(1)
                        .build()),
                // Clean Flocculation Casing
                ofBlock(GregTech_API.sBlockCasings9, 5)))
        // Water Plant Pipe Casing
        .addElement('C', ofBlock(GregTech_API.sBlockCasings9, 4))
        // Industrial Water Plant Casing
        .addElement('D', ofBlock(GregTech_API.sBlockCasings9, 3))
        .addElement('E', ofFrame(Materials.Adamantium))
        .addElement('W', ofBlock(Blocks.water, 0))
        // Tinted industrial glass
        .addElement('G', ofBlockAnyMeta(GregTech_API.sBlockTintedGlass))
        .build();

    List<IHatchElement<? super GT_MetaTileEntity_PurificationUnitFlocculation>> getAllowedHatches() {
        return ImmutableList.of(InputBus, InputHatch, OutputBus, OutputHatch);
    }

    public GT_MetaTileEntity_PurificationUnitFlocculation(int aID, String aName, String aNameRegional) {
        super(aID, aName, aNameRegional);
    }

    public GT_MetaTileEntity_PurificationUnitFlocculation(String aName) {
        super(aName);
    }

    @Override
    public IMetaTileEntity newMetaEntity(IGregTechTileEntity aTileEntity) {
        return new GT_MetaTileEntity_PurificationUnitFlocculation(this.mName);
    }

    @Override
    public ITexture[] getTexture(IGregTechTileEntity aBaseMetaTileEntity, ForgeDirection side, ForgeDirection aFacing,
        int colorIndex, boolean aActive, boolean redstoneLevel) {
        if (side == aFacing) {
            if (aActive) return new ITexture[] { Textures.BlockIcons.getCasingTextureForId(MAIN_CASING_INDEX),
                TextureFactory.builder()
                    .addIcon(OVERLAY_FRONT_LARGE_CHEMICAL_REACTOR_ACTIVE)
                    .extFacing()
                    .build(),
                TextureFactory.builder()
                    .addIcon(OVERLAY_FRONT_LARGE_CHEMICAL_REACTOR_ACTIVE_GLOW)
                    .extFacing()
                    .glow()
                    .build() };
            return new ITexture[] { Textures.BlockIcons.getCasingTextureForId(MAIN_CASING_INDEX),
                TextureFactory.builder()
                    .addIcon(OVERLAY_FRONT_LARGE_CHEMICAL_REACTOR)
                    .extFacing()
                    .build(),
                TextureFactory.builder()
                    .addIcon(OVERLAY_FRONT_LARGE_CHEMICAL_REACTOR_GLOW)
                    .extFacing()
                    .glow()
                    .build() };
        }
        return new ITexture[] { Textures.BlockIcons.getCasingTextureForId(MAIN_CASING_INDEX) };
    }

    @Override
    public void construct(ItemStack stackSize, boolean hintsOnly) {
        buildPiece(
            STRUCTURE_PIECE_MAIN,
            stackSize,
            hintsOnly,
            STRUCTURE_X_OFFSET,
            STRUCTURE_Y_OFFSET,
            STRUCTURE_Z_OFFSET);
    }

    @Override
    public int survivalConstruct(ItemStack stackSize, int elementBudget, ISurvivalBuildEnvironment env) {
        int built = survivialBuildPiece(
            STRUCTURE_PIECE_MAIN_SURVIVAL,
            stackSize,
            STRUCTURE_X_OFFSET,
            STRUCTURE_Y_OFFSET,
            STRUCTURE_Z_OFFSET,
            elementBudget,
            env,
            true);
        if (built == -1) {
            GT_Utility.sendChatToPlayer(
                env.getActor(),
                EnumChatFormatting.GREEN + "Auto placing done ! Now go place the water yourself !");
            return 0;
        }
        return built;
    }

    @Override
    public IStructureDefinition<GT_MetaTileEntity_PurificationUnitFlocculation> getStructureDefinition() {
        return STRUCTURE_DEFINITION;
    }

    @Override
    protected IAlignmentLimits getInitialAlignmentLimits() {
        // Do not allow rotation when water would flow out
        return (d, r, f) -> d.offsetY == 0 && r.isNotRotated() && !f.isVerticallyFliped();
    }

    public boolean checkMachine(IGregTechTileEntity aBaseMetaTileEntity, ItemStack aStack) {
        if (!checkPiece(STRUCTURE_PIECE_MAIN, STRUCTURE_X_OFFSET, STRUCTURE_Y_OFFSET, STRUCTURE_Z_OFFSET)) return false;

        // At most two input hatches allowed
        if (mInputHatches.size() > 2) {
            return false;
        }

        // At most two output hatches allowed
        if (mOutputHatches.size() > 2) {
            return false;
        }

        return super.checkMachine(aBaseMetaTileEntity, aStack);
    }

    @Override
    protected GT_Multiblock_Tooltip_Builder createTooltip() {
        final GT_Multiblock_Tooltip_Builder tt = new GT_Multiblock_Tooltip_Builder();
        tt.addMachineType("Purification Unit")
            .addInfo(
                EnumChatFormatting.AQUA + ""
                    + EnumChatFormatting.BOLD
                    + "Water Tier: "
                    + EnumChatFormatting.WHITE
                    + GT_Utility.formatNumbers(getWaterTier())
                    + EnumChatFormatting.RESET)
            .addInfo("Controller block for the Flocculation Purification Unit.")
            .addInfo("Must be linked to a Purification Plant to work.")
            .addSeparator()
            .addInfo("Filters out smaller dusts, algae and microplastics by mixing in chemicals.")
            .addInfo(
                "Supply with " + EnumChatFormatting.WHITE
                    + INPUT_CHEMICAL.mLocalizedName
                    + EnumChatFormatting.GRAY
                    + " to operate.")
            .addInfo(
                "Outputs " + EnumChatFormatting.WHITE
                    + OUTPUT_WASTE.mLocalizedName
                    + EnumChatFormatting.GRAY
                    + " that can be recycled.")
            .addSeparator()
            .addInfo(
                "During operation, will consume ALL " + EnumChatFormatting.WHITE
                    + INPUT_CHEMICAL.mLocalizedName
                    + EnumChatFormatting.GRAY
                    + " in the input hatch.")
            .addInfo(
                "At the end of the recipe, for every " + EnumChatFormatting.RED
                    + INPUT_CHEMICAL_PER_LEVEL
                    + "L "
                    + EnumChatFormatting.GRAY
                    + "of "
                    + EnumChatFormatting.WHITE
                    + INPUT_CHEMICAL.mLocalizedName
                    + EnumChatFormatting.GRAY
                    + " consumed")
            .addInfo(
                "gain an additive " + EnumChatFormatting.RED
                    + SUCCESS_PER_LEVEL
                    + "%"
                    + EnumChatFormatting.GRAY
                    + " increase to success. If total fluid supplied is not")
            .addInfo(
                "a multiple of " + EnumChatFormatting.RED
                    + INPUT_CHEMICAL_PER_LEVEL
                    + "L"
                    + EnumChatFormatting.GRAY
                    + ", a penalty to success is applied using the following formula:")
            .addInfo(EnumChatFormatting.GREEN + "Success = Success * 2^(-10 * Overflow ratio)")
            .addInfo(AuthorNotAPenguin)
            .beginStructureBlock(7, 4, 7, false)
            .addCasingInfoRangeColored(
                "Clean Flocculation Casing",
                EnumChatFormatting.GRAY,
                60,
                65,
                EnumChatFormatting.GOLD,
                false)
            .addCasingInfoExactlyColored(
                "Water Plant Pipe Casing",
                EnumChatFormatting.GRAY,
                16,
                EnumChatFormatting.GOLD,
                false)
            .addCasingInfoExactlyColored(
                "Industrial Water Plant Casing",
                EnumChatFormatting.GRAY,
                30,
                EnumChatFormatting.GOLD,
                false)
            .addCasingInfoExactlyColored(
                "Tinted Industrial Glass",
                EnumChatFormatting.GRAY,
                6,
                EnumChatFormatting.GOLD,
                false)
            .addCasingInfoExactlyColored(
                "Adamantium Frame Box",
                EnumChatFormatting.GRAY,
                12,
                EnumChatFormatting.GOLD,
                false)
            .addCasingInfoExactlyColored(
                "Filter Machine Casing",
                EnumChatFormatting.GRAY,
                9,
                EnumChatFormatting.GOLD,
                false)
            .addController("Front center")
            .addOutputBus(EnumChatFormatting.GOLD + "1" + EnumChatFormatting.GRAY + "+", 1)
            .addInputHatch(
                EnumChatFormatting.GOLD + "1" + EnumChatFormatting.GRAY + "-" + EnumChatFormatting.GOLD + "2",
                1)
            .addOutputHatch(
                EnumChatFormatting.GOLD + "1" + EnumChatFormatting.GRAY + "-" + EnumChatFormatting.GOLD + "2",
                1)
            .addStructureInfo("Use the StructureLib Hologram Projector to build the structure.")
            .toolTipFinisher("GregTech");
        return tt;
    }

    @NotNull
    @Override
    public CheckRecipeResult checkProcessing() {
        this.startRecipeProcessing();
        RecipeMap<?> recipeMap = this.getRecipeMap();

        GT_Recipe recipe = recipeMap.findRecipeQuery()
            .fluids(
                this.getStoredFluids()
                    .toArray(new FluidStack[] {}))
            .items(
                this.getStoredInputs()
                    .toArray(new ItemStack[] {}))
            .find();

        this.endRecipeProcessing();
        if (recipe == null) {
            return CheckRecipeResultRegistry.NO_RECIPE;
        }

        if (this.protectsExcessFluid() && !this.canOutputAll(recipe.mFluidOutputs)) {
            return CheckRecipeResultRegistry.FLUID_OUTPUT_FULL;
        }

        if (this.protectsExcessItem() && !this.canOutputAll(recipe.mOutputs)) {
            return CheckRecipeResultRegistry.ITEM_OUTPUT_FULL;
        }

        this.currentRecipe = recipe;
        return CheckRecipeResultRegistry.SUCCESSFUL;
    }

    @Override
    public void startCycle(int cycleTime, int progressTime) {
        super.startCycle(cycleTime, progressTime);
        this.inputFluidConsumed = 0;
    }

    @Override
    public void endCycle() {
        super.endCycle();
        // Output waste water proportional to amount of boost levels. We do this even when the recipe fails, so you can
        // always fully recycle.
        // NOTE: If this process ever PRODUCES excess chlorine, there is a recipe bug.
        int levels = calculateBoostLevels();
        long amount = levels * WASTE_WATER_PER_LEVEL;
        this.addFluidOutputs(new FluidStack[] { OUTPUT_WASTE.getFluid(amount) });
        this.inputFluidConsumed = 0;
    }

    @Override
    public void addRecipeOutputs() {
        super.addRecipeOutputs();
    }

    @Override
    protected void runMachine(IGregTechTileEntity aBaseMetaTileEntity, long aTick) {
        super.runMachine(aBaseMetaTileEntity, aTick);

        // Consume all input iron iii chloride periodically, only when running
        if (aTick % CONSUME_INTERVAL == 0 && mMaxProgresstime > 0) {
            // Iterate over all fluids stored
            List<FluidStack> fluids = this.getStoredFluids();
            for (int i = 0; i < fluids.size(); ++i) {
                FluidStack fluid = fluids.get(i);
                // If this FluidStack is the input chemical, consume it all
                if (fluid.getFluid()
                    .equals(INPUT_CHEMICAL.mFluid)) {
                    this.inputFluidConsumed += fluid.amount;
                    this.depleteInput(fluid);
                }
            }
        }
    }

    private int calculateBoostLevels() {
        return (int) Math.floor((float) this.inputFluidConsumed / (float) INPUT_CHEMICAL_PER_LEVEL);
    }

    @Override
    public float calculateFinalSuccessChance() {
        int levels = calculateBoostLevels();
        long targetAmount = levels * INPUT_CHEMICAL_PER_LEVEL;
        long overflow = inputFluidConsumed - targetAmount;
        float boost = SUCCESS_PER_LEVEL * levels;
        if (overflow > 0) {
            // Exponential penalty multiplier based on percentage overflow
            float overflowPct = (float) overflow / INPUT_CHEMICAL_PER_LEVEL;
            float penaltyMultiplier = (float) Math.pow(2.0f, overflowPct * -10.0);
            return Math.max(0.0f, (this.currentRecipeChance + boost) * penaltyMultiplier);
        } else {
            return Math.min(100.0f, this.currentRecipeChance + boost);
        }
    }

    @Override
    public boolean isCorrectMachinePart(ItemStack aStack) {
        return true;
    }

    @Override
    public int getWaterTier() {
        return 2;
    }

    @Override
    public long getActivePowerUsage() {
        return TierEU.RECIPE_LuV;
    }

    @Override
    public RecipeMap<?> getRecipeMap() {
        return RecipeMaps.purificationPlantGrade2Recipes;
    }

    @Override
    public String[] getInfoData() {
        ArrayList<String> infoData = new ArrayList<>(Arrays.asList(super.getInfoData()));
        infoData.add(
            INPUT_CHEMICAL.mLocalizedName + " consumed this cycle: "
                + EnumChatFormatting.RED
                + inputFluidConsumed
                + "L");
        return infoData.toArray(new String[] {});
    }

    @Override
    public void saveNBTData(NBTTagCompound aNBT) {
        super.saveNBTData(aNBT);
        aNBT.setLong("mInputFluidConsumed", inputFluidConsumed);
    }

    @Override
    public void loadNBTData(NBTTagCompound aNBT) {
        super.loadNBTData(aNBT);
        this.inputFluidConsumed = aNBT.getLong("mInputFluidConsumed");
    }

    @Override
    protected ResourceLocation getActivitySoundLoop() {
        return SoundResource.GT_MACHINES_COAGULATION_LOOP.resourceLocation;
    }
}
