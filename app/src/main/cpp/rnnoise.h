//
// Created by Betul on 4/26/2019.
//

#ifndef RNNOISE_EXPORT
# if defined(WIN32)
#  if defined(RNNOISE_BUILD) && defined(DLL_EXPORT)
#   define RNNOISE_EXPORT __declspec(dllexport)
#  else
#   define RNNOISE_EXPORT
#  endif
# elif defined(__GNUC__) && defined(RNNOISE_BUILD)
#  define RNNOISE_EXPORT __attribute__ ((visibility ("default")))
# else
#  define RNNOISE_EXPORT
# endif
#endif

typedef struct DenoiseState DenoiseState;

RNNOISE_EXPORT int rnnoise_get_size();

RNNOISE_EXPORT int rnnoise_init(DenoiseState *st);

RNNOISE_EXPORT DenoiseState *rnnoise_create();

RNNOISE_EXPORT void rnnoise_destroy(DenoiseState *st);

RNNOISE_EXPORT float rnnoise_process_frame(DenoiseState *st, float *out, const float *in);
