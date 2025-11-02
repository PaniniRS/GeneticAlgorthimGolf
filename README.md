<img width="1128" alt="image" src="https://github.com/user-attachments/assets/bcb3398d-861d-48a4-a140-4cbe34413d24" />

# Genetic Algorithm for Golf Shot Optimization

This project uses evolutionary computation to optimize golf ball trajectories under simulated physics. It evolves launch angle, velocity, and horizontal position to achieve a hole-in-one across varying distances. Three execution models are implemented to demonstrate algorithm performance across different computing environments.

This work showcases algorithm engineering, parallel computing, distributed systems, and computational experimentation.

---

## Highlights

- Full genetic algorithm pipeline with elitism, crossover, mutation
- Deterministic runs for reproducible results
- Real-time trajectory visualization (Java Swing GUI)
- Sequential, multithreaded, and MPI distributed versions
- CSV experiment output for analysis
- Benchmarking across 432 runs with multiple parameter sets

This repository reflects engineering rigor and academic methodology: performance measurement, control conditions, and system scalability evaluation.

---

## Technical Summary

| Feature | Details |
|---|---|
| Language | Java |
| Parallelism | ExecutorService, CyclicBarrier |
| Distributed Execution | MPI |
| Simulation | Gravity, drag, discrete physics |
| Visual Output | Swing GUI |
| Data Output | CSV logs |

### Chromosome
- Position: 0–100
- Velocity: 0–2500
- Angle: 0–180 degrees

### Physics
- Gravity = 9.8 m/s²
- Drag coefficient = 0.5
- Time-step integration

---

## Experimental Results (Executive Summary)

- Populations: 1000, 2000  
- Mutation: 0.1, 0.3, 0.6  
- Crossover: 0.2, 0.6  
- Elites: 4, 8  
- Targets: 300k, 500k, 800k  

Key outcomes:

- Multithreading yields ~40 percent average speedup
- Distributed MPI shows scaling potential for large workloads
- Convergence consistently achieved with proper parameter tuning
- Top performing settings: mutation ≈ 0.3, crossover ≈ 0.6, elites ≈ 8

---

## Running the Project

### Single-thread execution
```bash
java SingleThreaded
```

### Multithreaded execution
```bash
java GeneticGolf multi
```

### Distributed MPI execution
```bash
mpirun -np <process_count> java GeneticGolf distributed
```

GUI mode can be toggled for performance tests.

---

## Project Structure
src/
├── SingleThreaded.java
├── GeneticGolf.java
├── GUI.java
├── MultiThreaded/
└── MPI/
assets/
results/
README.md

---

## Future Potential Enhancements

- Adaptive mutation and crossover schedules
- Tournament and rank-based selection
- 3D physics with spin and wind
- GPU-accelerated fitness evaluation
- Hybrid GA + local optimization
- Real-time analytics panel in GUI

---

## Documentation

Full research report:

**GeneticAlgorithm Report.pdf**

Includes methodology, analysis, graphs, parameter study, and performance evaluation.

---

## Author

**Ljupche Gigov**  
Focus: simulation, concurrency, distributed computing, evolutionary algorithms.

---

