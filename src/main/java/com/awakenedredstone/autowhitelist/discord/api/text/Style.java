package com.awakenedredstone.autowhitelist.discord.api.text;

import com.awakenedredstone.autowhitelist.discord.api.util.Formatting;
import com.awakenedredstone.autowhitelist.discord.api.util.Identifier;
import com.awakenedredstone.autowhitelist.discord.api.util.InvalidIdentifierException;
import com.awakenedredstone.autowhitelist.discord.api.util.JsonHelper;
import com.google.gson.*;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.Objects;

/**
 * The style of a {@link Text}, representing cosmetic attributes. It includes
 * font, formatting, click/hover events (actions), color, etc.
 *
 * <p>A style is immutable.
 *
 * @see Text
 */
public record Style(@Nullable TextColor color, @Nullable Boolean bold,
                    @Nullable Boolean italic, @Nullable Boolean underlined,
                    @Nullable Boolean strikethrough, @Nullable Boolean obfuscated,
                    @Nullable String insertion, @Nullable Identifier font,
                    Formatting... formattings) {
    /**
     * An empty style.
     */
    public static final Style EMPTY = new Style(null, null, null, null, null, null, null, null);
    /**
     * The identifier for the default font of a style.
     */
    public static final Identifier DEFAULT_FONT_ID = new Identifier("minecraft", "default");

    public Style(@Nullable TextColor color, @Nullable Boolean bold, @Nullable Boolean italic, @Nullable Boolean underlined, @Nullable Boolean strikethrough, @Nullable Boolean obfuscated, @Nullable String insertion, @Nullable Identifier font, Formatting... formattings) {
        this.color = color;
        this.bold = bold;
        this.italic = italic;
        this.underlined = underlined;
        this.strikethrough = strikethrough;
        this.obfuscated = obfuscated;
        this.insertion = insertion;
        this.font = font;
        this.formattings = formattings;
    }

    /**
     * Returns the color of this style.
     */
    @Nullable
    public TextColor getColor() {
        return this.color;
    }

    /**
     * Returns whether the style has bold formatting.
     *
     * @see Formatting#BOLD
     */
    public boolean isBold() {
        return this.bold == Boolean.TRUE;
    }

    /**
     * Returns whether the style has italic formatting.
     *
     * @see Formatting#ITALIC
     */
    public boolean isItalic() {
        return this.italic == Boolean.TRUE;
    }

    /**
     * Returns whether the style has strikethrough formatting.
     *
     * @see Formatting#STRIKETHROUGH
     */
    public boolean isStrikethrough() {
        return this.strikethrough == Boolean.TRUE;
    }

    /**
     * Returns whether the style has underline formatting.
     *
     * @see Formatting#UNDERLINE
     */
    public boolean isUnderlined() {
        return this.underlined == Boolean.TRUE;
    }

    /**
     * Returns whether the style has obfuscated formatting.
     *
     * @see Formatting#OBFUSCATED
     */
    public boolean isObfuscated() {
        return this.obfuscated == Boolean.TRUE;
    }

    /**
     * Returns if this is the empty style.
     *
     * @see #EMPTY
     */
    public boolean isEmpty() {
        return this == EMPTY;
    }

    /**
     * Returns the insertion text of the style.
     *
     * <p>An insertion is inserted when a piece of text clicked while shift key
     * is down in the chat HUD.
     */
    @Nullable
    public String getInsertion() {
        return this.insertion;
    }

    /**
     * Returns the font of this style.
     */
    public Identifier getFont() {
        return this.font != null ? this.font : DEFAULT_FONT_ID;
    }

    /**
     * Returns a new style with the color provided and all other attributes of
     * this style.
     *
     * @param color the new color
     */
    public Style withColor(@Nullable TextColor color) {
        return new Style(color, this.bold, this.italic, this.underlined, this.strikethrough, this.obfuscated, this.insertion, this.font);
    }

    /**
     * Returns a new style with the color provided and all other attributes of
     * this style.
     *
     * @param color the new color
     */
    public Style withColor(@Nullable Formatting color) {
        return this.withColor(color != null ? TextColor.fromFormatting(color) : null);
    }

    public Style withColor(int rgbColor) {
        return this.withColor(TextColor.fromRgb(rgbColor));
    }

    /**
     * Returns a new style with the bold attribute provided and all other
     * attributes of this style.
     *
     * @param bold the new bold property
     */
    public Style withBold(@Nullable Boolean bold) {
        return new Style(this.color, bold, this.italic, this.underlined, this.strikethrough, this.obfuscated, this.insertion, this.font);
    }

    /**
     * Returns a new style with the italic attribute provided and all other
     * attributes of this style.
     *
     * @param italic the new italic property
     */
    public Style withItalic(@Nullable Boolean italic) {
        return new Style(this.color, this.bold, italic, this.underlined, this.strikethrough, this.obfuscated, this.insertion, this.font);
    }

    /**
     * Returns a new style with the underline attribute provided and all other
     * attributes of this style.
     */
    public Style withUnderline(@Nullable Boolean underline) {
        return new Style(this.color, this.bold, this.italic, underline, this.strikethrough, this.obfuscated, this.insertion, this.font);
    }

    public Style withStrikethrough(@Nullable Boolean strikethrough) {
        return new Style(this.color, this.bold, this.italic, this.underlined, strikethrough, this.obfuscated, this.insertion, this.font);
    }

    public Style obfuscated(@Nullable Boolean obfuscated) {
        return new Style(this.color, this.bold, this.italic, this.underlined, this.strikethrough, obfuscated, this.insertion, this.font);
    }

    /**
     * Returns a new style with the insertion provided and all other
     * attributes of this style.
     *
     * @param insertion the new insertion string
     */
    public Style withInsertion(@Nullable String insertion) {
        return new Style(this.color, this.bold, this.italic, this.underlined, this.strikethrough, this.obfuscated, insertion, this.font);
    }

    /**
     * Returns a new style with the font provided and all other
     * attributes of this style.
     *
     * @param font the new font
     */
    public Style withFont(@Nullable Identifier font) {
        return new Style(this.color, this.bold, this.italic, this.underlined, this.strikethrough, this.obfuscated, this.insertion, font);
    }

    /**
     * Returns a new style with the formatting provided and all other
     * attributes of this style.
     *
     * @param formatting the new formatting
     */
    public Style withFormatting(Formatting formatting) {
        TextColor textColor = this.color;
        Boolean boolean_ = this.bold;
        Boolean boolean2 = this.italic;
        Boolean boolean3 = this.strikethrough;
        Boolean boolean4 = this.underlined;
        Boolean boolean5 = this.obfuscated;
        switch (formatting) {
            case OBFUSCATED:
                boolean5 = true;
                break;
            case BOLD:
                boolean_ = true;
                break;
            case STRIKETHROUGH:
                boolean3 = true;
                break;
            case UNDERLINE:
                boolean4 = true;
                break;
            case ITALIC:
                boolean2 = true;
                break;
            case RESET:
                return EMPTY;
            default:
                textColor = TextColor.fromFormatting(formatting);
        }

        return new Style(textColor, boolean_, boolean2, boolean4, boolean3, boolean5, this.insertion, this.font, formatting);
    }

    /**
     * Returns a new style with the formatting provided and some applicable
     * attributes of this style.
     *
     * <p>When a color formatting is passed for {@code formatting}, the other
     * formattings, including bold, italic, strikethrough, underlined, and
     * obfuscated, are all removed.
     *
     * @param formatting the new formatting
     */
    public Style withExclusiveFormatting(Formatting formatting) {
        TextColor textColor = this.color;
        Boolean boolean_ = this.bold;
        Boolean boolean2 = this.italic;
        Boolean boolean3 = this.strikethrough;
        Boolean boolean4 = this.underlined;
        Boolean boolean5 = this.obfuscated;
        switch (formatting) {
            case OBFUSCATED:
                boolean5 = true;
                break;
            case BOLD:
                boolean_ = true;
                break;
            case STRIKETHROUGH:
                boolean3 = true;
                break;
            case UNDERLINE:
                boolean4 = true;
                break;
            case ITALIC:
                boolean2 = true;
                break;
            case RESET:
                return EMPTY;
            default:
                boolean5 = false;
                boolean_ = false;
                boolean3 = false;
                boolean4 = false;
                boolean2 = false;
                textColor = TextColor.fromFormatting(formatting);
        }

        return new Style(textColor, boolean_, boolean2, boolean4, boolean3, boolean5, this.insertion, this.font, formatting);
    }

    /**
     * Returns a new style with the formattings provided and all other
     * attributes of this style.
     *
     * @param formattings an array of new formattings
     */
    public Style withFormatting(Formatting... formattings) {
        TextColor textColor = this.color;
        Boolean boolean_ = this.bold;
        Boolean boolean2 = this.italic;
        Boolean boolean3 = this.strikethrough;
        Boolean boolean4 = this.underlined;
        Boolean boolean5 = this.obfuscated;
        Formatting[] var8 = formattings;
        int var9 = formattings.length;

        for (int var10 = 0; var10 < var9; ++var10) {
            Formatting formatting = var8[var10];
            switch (formatting) {
                case OBFUSCATED:
                    boolean5 = true;
                    break;
                case BOLD:
                    boolean_ = true;
                    break;
                case STRIKETHROUGH:
                    boolean3 = true;
                    break;
                case UNDERLINE:
                    boolean4 = true;
                    break;
                case ITALIC:
                    boolean2 = true;
                    break;
                case RESET:
                    return EMPTY;
                default:
                    textColor = TextColor.fromFormatting(formatting);
            }
        }

        return new Style(textColor, boolean_, boolean2, boolean4, boolean3, boolean5, this.insertion, this.font, formattings);
    }

    /**
     * Returns a new style with the undefined attributes of this style filled
     * by the {@code parent} style.
     *
     * @param parent the parent style
     */
    public Style withParent(Style parent) {
        if (this == EMPTY) {
            return parent;
        } else {
            return parent == EMPTY ? this : new Style(this.color != null ? this.color : parent.color, this.bold != null ? this.bold : parent.bold, this.italic != null ? this.italic : parent.italic, this.underlined != null ? this.underlined : parent.underlined, this.strikethrough != null ? this.strikethrough : parent.strikethrough, this.obfuscated != null ? this.obfuscated : parent.obfuscated, this.insertion != null ? this.insertion : parent.insertion, this.font != null ? this.font : parent.font, this.formattings != null ? this.formattings : parent.formattings);
        }
    }

    public String toString() {
        return "Style{ color=" + this.color + ", bold=" + this.bold + ", italic=" + this.italic + ", underlined=" + this.underlined + ", strikethrough=" + this.strikethrough + ", obfuscated=" + this.obfuscated + ", insertion=" + this.getInsertion() + ", font=" + this.getFont() + ", formattings=" + Arrays.toString(this.formattings) + "}";
    }

    public boolean equals(Object o) {
        if (this == o) {
            return true;
        } else if (!(o instanceof Style style)) {
            return false;
        } else {
            return this.isBold() == style.isBold() && Objects.equals(this.getColor(), style.getColor()) && this.isItalic() == style.isItalic() && this.isObfuscated() == style.isObfuscated() && this.isStrikethrough() == style.isStrikethrough() && this.isUnderlined() == style.isUnderlined() && Objects.equals(this.getInsertion(), style.getInsertion()) && Objects.equals(this.getFont(), style.getFont()) && Arrays.equals(this.formattings, style.formattings);
        }
    }

    public int hashCode() {
        return Objects.hash(this.color, this.bold, this.italic, this.underlined, this.strikethrough, this.obfuscated, this.insertion);
    }

    /**
     * A JSON serializer for {@link Style}.
     */
    public static class Serializer implements JsonDeserializer<Style>, JsonSerializer<Style> {
        @Nullable
        public Style deserialize(JsonElement jsonElement, Type type, JsonDeserializationContext jsonDeserializationContext) throws JsonParseException {
            if (jsonElement.isJsonObject()) {
                JsonObject jsonObject = jsonElement.getAsJsonObject();
                if (jsonObject == null) {
                    return null;
                } else {
                    Boolean boolean_ = parseNullableBoolean(jsonObject, "bold");
                    Boolean boolean2 = parseNullableBoolean(jsonObject, "italic");
                    Boolean boolean3 = parseNullableBoolean(jsonObject, "underlined");
                    Boolean boolean4 = parseNullableBoolean(jsonObject, "strikethrough");
                    Boolean boolean5 = parseNullableBoolean(jsonObject, "obfuscated");
                    TextColor textColor = parseColor(jsonObject);
                    String string = parseInsertion(jsonObject);
                    Identifier identifier = getFont(jsonObject);
                    return new Style(textColor, boolean_, boolean2, boolean3, boolean4, boolean5, string, identifier);
                }
            } else {
                return null;
            }
        }

        @Nullable
        private static Identifier getFont(JsonObject root) {
            if (root.has("font")) {
                String string = JsonHelper.getString(root, "font");

                try {
                    return new Identifier(string);
                } catch (InvalidIdentifierException var3) {
                    throw new JsonSyntaxException("Invalid font name: " + string);
                }
            } else {
                return null;
            }
        }

        @Nullable
        private static String parseInsertion(JsonObject root) {
            return JsonHelper.getString(root, "insertion", null);
        }

        @Nullable
        private static TextColor parseColor(JsonObject root) {
            if (root.has("color")) {
                String string = JsonHelper.getString(root, "color");
                return TextColor.parse(string);
            } else {
                return null;
            }
        }

        @Nullable
        private static Boolean parseNullableBoolean(JsonObject root, String key) {
            return root.has(key) ? root.get(key).getAsBoolean() : null;
        }

        @Nullable
        public JsonElement serialize(Style style, Type type, JsonSerializationContext jsonSerializationContext) {
            if (style.isEmpty()) {
                return null;
            } else {
                JsonObject jsonObject = new JsonObject();
                if (style.bold != null) {
                    jsonObject.addProperty("bold", style.bold);
                }

                if (style.italic != null) {
                    jsonObject.addProperty("italic", style.italic);
                }

                if (style.underlined != null) {
                    jsonObject.addProperty("underlined", style.underlined);
                }

                if (style.strikethrough != null) {
                    jsonObject.addProperty("strikethrough", style.strikethrough);
                }

                if (style.obfuscated != null) {
                    jsonObject.addProperty("obfuscated", style.obfuscated);
                }

                if (style.color != null) {
                    jsonObject.addProperty("color", style.color.getName());
                }

                if (style.insertion != null) {
                    jsonObject.add("insertion", jsonSerializationContext.serialize(style.insertion));
                }

                if (style.font != null) {
                    jsonObject.addProperty("font", style.font.toString());
                }

                return jsonObject;
            }
        }
    }
}
