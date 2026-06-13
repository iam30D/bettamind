#ifndef BETTAMIND_SQLCIPHER_H
#define BETTAMIND_SQLCIPHER_H

#ifndef SQLITE_HAS_CODEC
#define SQLITE_HAS_CODEC
#endif

#include <sqlite3.h>

static inline sqlite3_destructor_type bettamind_sqlcipher_transient(void) {
    return SQLITE_TRANSIENT;
}

#endif
