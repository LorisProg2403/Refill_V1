package net.loris.refillv1;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.SlotActionType;

public class Refill_V1Client implements ClientModInitializer {
    private static boolean previousHandEmpty = false; // État précédent de la main
    private static Item lastHeldItem = null; // Dernier item tenu avant d'être consommé

    @Override
    public void onInitializeClient() {
        // Événement déclenché à chaque tick du jeu
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            ClientPlayerEntity player = MinecraftClient.getInstance().player;
            if (player != null) {
                if (!isHandEmpty(player)) {
                    // Met à jour le dernier item tenu (tant que la main n'est pas vide)
                    lastHeldItem = player.getMainHandStack().getItem();
                }

                boolean currentHandEmpty = isHandEmpty(player);

                // Vérifier seulement si l'état de la main a changé
                if (currentHandEmpty != previousHandEmpty) {
                    if (currentHandEmpty && lastHeldItem != null) {
                        boolean foundInInventory = hasItemInInventory(player, lastHeldItem);
                        System.out.println("Main vide, item trouvé dans l'inventaire : " + foundInInventory);

                        if (foundInInventory) {
                            moveItemToMainHand(player, lastHeldItem);
                        }
                    }
                    previousHandEmpty = currentHandEmpty; // Met à jour l'état précédent
                }
            }
        });
    }

    // Vérifie si la main principale du joueur est vide
    public static boolean isHandEmpty(PlayerEntity player) {
        return player.getMainHandStack().isEmpty();
    }

    // Vérifie si l'inventaire du joueur contient un item donné
    public static boolean hasItemInInventory(PlayerEntity player, Item item) {
        for (int i = 9; i < player.getInventory().size(); i++) { // On commence après la hotbar
            ItemStack stack = player.getInventory().getStack(i);
            if (!stack.isEmpty() && stack.getItem() == item) {
                return true; // L'item est trouvé dans l'inventaire
            }
        }
        return false; // L'item n'est pas présent dans l'inventaire
    }

    // Déplace un item de l’inventaire vers la main avec validation serveur
    public static void moveItemToMainHand(PlayerEntity player, Item item) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.interactionManager == null) return;

        for (int i = 9; i < player.getInventory().size(); i++) { // On commence après la hotbar
            ItemStack stack = player.getInventory().getStack(i);

            if (!stack.isEmpty() && stack.getItem() == item) {
                int hotbarSlot = findEmptyHotbarSlot(player);
                if (hotbarSlot == -1) {
                    System.out.println("Pas de place dans la hotbar !");
                    return;
                }

                // 💥 Nouvelle technique : Envoyer une action d’échange à l’inventaire 💥
                client.interactionManager.clickSlot(player.playerScreenHandler.syncId, i, hotbarSlot, SlotActionType.SWAP, player);

                // Sélectionner le slot où l’item a été déplacé
                player.getInventory().selectedSlot = hotbarSlot;

                System.out.println("Item déplacé dans la main !");
                return;
            }
        }
        System.out.println("Aucun item trouvé pour refill.");
    }

    // Trouve un slot vide dans la hotbar pour y déplacer l'item
    private static int findEmptyHotbarSlot(PlayerEntity player) {
        for (int i = 0; i < 9; i++) { // La hotbar a 9 slots (0 à 8)
            if (player.getInventory().getStack(i).isEmpty()) {
                return i;
            }
        }
        return -1; // Pas de place libre
    }
}













