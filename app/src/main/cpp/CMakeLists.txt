cmake_minimum_required(VERSION 3.22)
project(gtbridge CXX)
set(CMAKE_CXX_STANDARD 17)

add_library(gtbridge SHARED bridge.cpp)
find_library(log-lib log)
target_link_libraries(gtbridge android ${log-lib})

# Quando existir app/src/main/cpp/core/ (código gerado pela recompilação + runtime),
# ele é compilado e ligado automaticamente.
if(EXISTS ${CMAKE_CURRENT_SOURCE_DIR}/core/CMakeLists.txt)
  add_subdirectory(core)                       # deve definir o alvo "gtcore"
  target_compile_definitions(gtbridge PRIVATE GT_RECOMP_CORE=1)
  target_link_libraries(gtbridge gtcore)
endif()
