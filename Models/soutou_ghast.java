// Made with Blockbench 5.0.7
// Exported for Minecraft version 1.17 or later with Mojang mappings
// Paste this class into your mod and generate all required imports


public class soutou_ghast<T extends Entity> extends EntityModel<T> {
	// This layer location should be baked with EntityRendererProvider.Context in the entity renderer and passed into this model's constructor
	public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(new ResourceLocation("modid", "soutou_ghast"), "main");
	private final ModelPart bone2;
	private final ModelPart bone;

	public soutou_ghast(ModelPart root) {
		this.bone2 = root.getChild("bone2");
		this.bone = root.getChild("bone");
	}

	public static LayerDefinition createBodyLayer() {
		MeshDefinition meshdefinition = new MeshDefinition();
		PartDefinition partdefinition = meshdefinition.getRoot();

		PartDefinition bone2 = partdefinition.addOrReplaceChild("bone2", CubeListBuilder.create().texOffs(48, 4).addBox(-2.0F, 7.0F, 1.0F, 4.0F, 2.1F, 1.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 9.5F, -10.0F));

		PartDefinition bone = partdefinition.addOrReplaceChild("bone", CubeListBuilder.create().texOffs(11, 1).mirror().addBox(-8.0F, -2.0F, 2.0F, 16.0F, 0.5F, 16.0F, new CubeDeformation(0.0F)).mirror(false)
		.texOffs(24, 1).addBox(-8.0F, -2.0F, 1.0F, 16.0F, 2.0F, 1.0F, new CubeDeformation(0.0F))
		.texOffs(1, 1).mirror().addBox(-1.0F, 0.0F, 1.0F, 9.0F, 1.0F, 1.0F, new CubeDeformation(0.0F)).mirror(false)
		.texOffs(1, 1).mirror().addBox(2.0F, 1.0F, 1.0F, 6.0F, 1.0F, 1.0F, new CubeDeformation(0.0F)).mirror(false)
		.texOffs(1, 1).mirror().addBox(5.0F, 2.0F, 1.0F, 3.0F, 1.3F, 1.0F, new CubeDeformation(0.0F)).mirror(false)
		.texOffs(0, 1).addBox(8.0F, -2.0F, 1.0F, 0.5F, 7.8F, 17.0F, new CubeDeformation(0.0F))
		.texOffs(0, 1).addBox(8.0F, 5.8F, 13.0F, 0.5F, 1.6F, 5.0F, new CubeDeformation(0.0F))
		.texOffs(0, 1).addBox(-8.3F, -2.0F, 1.0F, 0.3F, 2.0F, 17.0F, new CubeDeformation(0.0F))
		.texOffs(0, 1).addBox(-8.3F, 0.0F, 15.3F, 0.3F, 5.7F, 2.7F, new CubeDeformation(0.0F))
		.texOffs(0, 1).addBox(-8.3F, 0.0F, 7.9F, 0.3F, 3.6F, 7.4F, new CubeDeformation(0.0F))
		.texOffs(0, 1).addBox(-8.3F, -2.0F, 18.0F, 16.8F, 9.4F, 0.8F, new CubeDeformation(0.0F))
		.texOffs(0, 1).addBox(-6.3F, 7.4F, 18.0F, 12.8F, 1.4F, 0.8F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 9.5F, -10.0F));

		return LayerDefinition.create(meshdefinition, 64, 32);
	}

	@Override
	public void setupAnim(Entity entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {

	}

	@Override
	public void renderToBuffer(PoseStack poseStack, VertexConsumer vertexConsumer, int packedLight, int packedOverlay, float red, float green, float blue, float alpha) {
		bone2.render(poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha);
		bone.render(poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha);
	}
}