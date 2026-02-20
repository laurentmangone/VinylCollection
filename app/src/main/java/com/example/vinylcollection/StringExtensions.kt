package com.example.vinylcollection

import java.text.Normalizer

/**
 * Extension function pour normaliser une chaîne de caractères
 * Supprime les accents et met en minuscule pour une recherche insensible aux accents
 */
fun String.normalizeForSearch(): String {
    return Normalizer.normalize(this, Normalizer.Form.NFD)
        .replace(Regex("\\p{InCombiningDiacriticalMarks}+"), "")
        .lowercase()
}

/**
 * Vérifie si cette chaîne contient la query (insensible aux accents et à la casse)
 */
@Suppress("unused")
fun String.containsNormalized(query: String): Boolean {
    return this.normalizeForSearch().contains(query.normalizeForSearch())
}
