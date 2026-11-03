<template>
    <main class="access-denied" role="alert">
        <section class="access-denied__card">
            <div class="access-denied__icon" aria-hidden="true">!</div>
            <p class="access-denied__eyebrow">Accès refusé · 403</p>
            <h1>Vous n’avez pas la permission d’accéder à cette page</h1>
            <p>
                Votre poste ne dispose pas des autorisations nécessaires. Si cet accès est requis,
                contactez l’administrateur de votre application.
            </p>
            <router-link to="/dashboard" class="access-denied__button">Retour au tableau de bord</router-link>
        </section>
    </main>
</template>

<style scoped>
.access-denied {
    min-height: 100vh;
    display: grid;
    place-items: center;
    padding: 2rem;
    background: #f4f7fb;
    color: #17233c;
    font-family: Inter, system-ui, sans-serif;
}
.access-denied__card {
    width: min(100%, 34rem);
    padding: clamp(2rem, 6vw, 3.5rem);
    border: 1px solid #e0e7f0;
    border-radius: 1.25rem;
    background: white;
    box-shadow: 0 1.5rem 4rem #1e33501a;
    text-align: center;
}
.access-denied__icon {
    display: grid;
    width: 3.5rem;
    height: 3.5rem;
    margin: 0 auto 1.25rem;
    place-items: center;
    border-radius: 50%;
    background: #fff0e8;
    color: #c34b18;
    font-size: 1.75rem;
    font-weight: 800;
}
.access-denied__eyebrow {
    margin: 0 0 .5rem;
    color: #a3421d;
    font-size: .75rem;
    font-weight: 800;
    letter-spacing: .12em;
    text-transform: uppercase;
}
h1 {
    margin: 0;
    font-size: clamp(1.5rem, 4vw, 2rem);
    line-height: 1.2;
}
.access-denied__card > p:not(.access-denied__eyebrow) {
    margin: 1rem 0 1.75rem;
    color: #63718a;
    line-height: 1.65;
}
.access-denied__button {
    display: inline-flex;
    align-items: center;
    justify-content: center;
    padding: .8rem 1.1rem;
    border-radius: .7rem;
    background: #1a2a6c;
    color: white;
    font-weight: 700;
    text-decoration: none;
}
.access-denied__button:hover { background: #243b91; }
</style>
