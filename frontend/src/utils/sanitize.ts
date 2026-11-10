import DOMPurify from 'dompurify';

/**
 * Sanitiza cualquier HTML proveniente del backend o el usuario antes de renderizarlo.
 * Úsalo SIEMPRE con dangerouslySetInnerHTML.
 */
export const sanitizeHtml = (dirty: string): string => {
    return DOMPurify.sanitize(dirty, {
        ALLOWED_TAGS: ['b', 'i', 'em', 'strong', 'a', 'p', 'br', 'ul', 'li'],
        ALLOWED_ATTR: ['href', 'target', 'rel'],
    });
};

/**
 * Limpia strings simples (elimina caracteres potencialmente peligrosos).
 */
export const sanitizeString = (input: string): string => {
    return input.replace(/[<>"'`]/g, '').trim();
};