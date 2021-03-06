package com.velocitypowered.proxy.protocol.packet.brigadier;

import static com.velocitypowered.proxy.protocol.packet.brigadier.DoubleArgumentPropertySerializer.DOUBLE;
import static com.velocitypowered.proxy.protocol.packet.brigadier.EmptyArgumentPropertySerializer.EMPTY;
import static com.velocitypowered.proxy.protocol.packet.brigadier.FloatArgumentPropertySerializer.FLOAT;
import static com.velocitypowered.proxy.protocol.packet.brigadier.IntegerArgumentPropertySerializer.INTEGER;
import static com.velocitypowered.proxy.protocol.packet.brigadier.LongArgumentPropertySerializer.LONG;
import static com.velocitypowered.proxy.protocol.packet.brigadier.StringArgumentPropertySerializer.STRING;

import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.LongArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.velocitypowered.proxy.protocol.ProtocolUtils;
import io.netty.buffer.ByteBuf;
import java.util.HashMap;
import java.util.Map;

public class ArgumentPropertyRegistry {
  private ArgumentPropertyRegistry() {
    throw new AssertionError();
  }

  private static final Map<String, ArgumentPropertySerializer<?>> byId = new HashMap<>();
  private static final Map<Class<? extends ArgumentType>,
      ArgumentPropertySerializer<?>> byClass = new HashMap<>();
  private static final Map<Class<? extends ArgumentType>, String> classToId = new HashMap<>();

  private static <T extends ArgumentType<?>> void register(String identifier, Class<T> klazz,
      ArgumentPropertySerializer<T> serializer) {
    byId.put(identifier, serializer);
    byClass.put(klazz, serializer);
    classToId.put(klazz, identifier);
  }

  private static <T> void empty(String identifier) {
    empty(identifier, EMPTY);
  }

  private static <T> void empty(String identifier, ArgumentPropertySerializer<T> serializer) {
    byId.put(identifier, serializer);
  }

  /**
   * Deserializes the {@link ArgumentType}.
   * @param buf the buffer to deserialize
   * @return the deserialized {@link ArgumentType}
   */
  public static ArgumentType<?> deserialize(ByteBuf buf) {
    String identifier = ProtocolUtils.readString(buf);
    ArgumentPropertySerializer<?> serializer = byId.get(identifier);
    if (serializer == null) {
      throw new IllegalArgumentException("Argument type identifier " + identifier + " unknown.");
    }
    Object result = serializer.deserialize(buf);

    if (result instanceof ArgumentType) {
      return (ArgumentType<?>) result;
    } else {
      return new PassthroughProperty(identifier, serializer, result);
    }
  }

  /**
   * Serializes the {@code type} into the provided {@code buf}.
   * @param buf the buffer to serialize into
   * @param type the type to serialize
   */
  public static void serialize(ByteBuf buf, ArgumentType<?> type) {
    if (type instanceof PassthroughProperty) {
      PassthroughProperty property = (PassthroughProperty) type;
      ProtocolUtils.writeString(buf, property.getIdentifier());
      if (property.getResult() != null) {
        property.getSerializer().serialize(property.getResult(), buf);
      }
    } else {
      ArgumentPropertySerializer serializer = byClass.get(type.getClass());
      String id = classToId.get(type.getClass());
      if (serializer == null || id == null) {
        throw new IllegalArgumentException("Don't know how to serialize "
            + type.getClass().getName());
      }
      ProtocolUtils.writeString(buf, id);
      serializer.serialize(type, buf);
    }
  }

  static {
    // Base Brigadier argument types
    register("brigadier:string", StringArgumentType.class, STRING);
    register("brigadier:integer", IntegerArgumentType.class, INTEGER);
    register("brigadier:float", FloatArgumentType.class, FLOAT);
    register("brigadier:double", DoubleArgumentType.class, DOUBLE);
    register("brigadier:bool", BoolArgumentType.class,
        GenericArgumentPropertySerializer.create(BoolArgumentType::bool));
    register("brigadier:long", LongArgumentType.class, LONG);

    // Minecraft argument types with extra properties
    empty("minecraft:entity", ByteArgumentPropertySerializer.BYTE);
    empty("minecraft:score_holder", ByteArgumentPropertySerializer.BYTE);

    // Minecraft argument types
    empty("minecraft:game_profile");
    empty("minecraft:block_pos");
    empty("minecraft:column_pos");
    empty("minecraft:vec3");
    empty("minecraft:vec2");
    empty("minecraft:block_state");
    empty("minecraft:block_predicate");
    empty("minecraft:item_stack");
    empty("minecraft:item_predicate");
    empty("minecraft:color");
    empty("minecraft:component");
    empty("minecraft:message");
    empty("minecraft:nbt");
    empty("minecraft:nbt_compound_tag"); // added in 1.14
    empty("minecraft:nbt_tag"); // added in 1.14
    empty("minecraft:nbt_path");
    empty("minecraft:objective");
    empty("minecraft:objective_criteria");
    empty("minecraft:operation");
    empty("minecraft:particle");
    empty("minecraft:rotation");
    empty("minecraft:scoreboard_slot");
    empty("minecraft:swizzle");
    empty("minecraft:team");
    empty("minecraft:item_slot");
    empty("minecraft:resource_location");
    empty("minecraft:mob_effect");
    empty("minecraft:function");
    empty("minecraft:entity_anchor");
    empty("minecraft:item_enchantment");
    empty("minecraft:entity_summon");
    empty("minecraft:dimension");
    empty("minecraft:int_range");
    empty("minecraft:float_range");
    empty("minecraft:time"); // added in 1.14
    empty("minecraft:uuid"); // added in 1.16
    empty("minecraft:angle"); // added in 1.16.2
  }
}
