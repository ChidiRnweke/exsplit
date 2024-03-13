<script>
	import '../app.css';
	import Footer from '../components/shared/Footer.svelte';
	import Main from '../components/shared/Main.svelte';
	import Nav from '../components/shared/Nav.svelte';
</script>

<Nav class="sticky top-0 z-50 mb-10 border-b-2" />

<Main>
	<slot />
</Main>

<Footer class="mb-10 ml-5 mr-5 mt-10 text-center lg:ml-56 lg:mr-56" />
