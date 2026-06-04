package com.notetask;

import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NoteTaskMod implements ModInitializer {
    public static final String MOD_ID = "notetask";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        LOGGER.info("NoteTask loaded");
    }
}
